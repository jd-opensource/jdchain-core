package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;
import com.jd.blockchain.contract.jvm.rust.Result;
import com.jd.blockchain.contract.jvm.rust.request.*;
import com.jd.blockchain.contract.jvm.rust.result.*;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.ledger.*;
import org.wasmer.*;
import org.wasmer.exports.Function;
import utils.Bytes;
import utils.io.BytesUtils;
import utils.serialize.json.JSONSerializeUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wasm 运行时 Rust 合约源码加载合约；
 */
public class RustContractCode extends AbstractContractCode {
    private byte[] chainCode;
    // 记录执行异常
    private Exception contractException;

    public RustContractCode(Bytes address, long version, byte[] chainCode) {
        super(address, version);
        this.chainCode = chainCode;
    }

    protected Object getContractInstance(ContractEventContext eventContext) {
        Module module = new Module(chainCode);
        try {
            AtomicReference<Instance> arInstance = new AtomicReference<>();
            AtomicReference<byte[]> contractMsg = new AtomicReference<>();
            /**
             *  账本数据库交互
             *  Rust合约与账本数据库交互分两步进行：
             *  1. sys_call 生成带接受执行结果信息，运行时缓存执行结果，返回信息长度
             *  2. sys_msg 获取上一次 sys_call 执行结果信息
             */
            Imports imports = Imports.from(Arrays.asList(new Imports.Spec("sys_call", argv -> {
                Memory memory = arInstance.get().exports.getMemory("memory");
                int reqPtr = argv.get(1).intValue();
                ByteBuffer mbf = memory.buffer();
                try {
                    String req = getString(reqPtr, mbf);
                    Result result = sysCall(eventContext, req);
                    if (null != result) {
                        String json = JSONSerializeUtils.serializeToJSON(result);
                        byte[] data = json.getBytes(StandardCharsets.UTF_8);
                        contractMsg.set(data);
                        argv.set(0, data.length);
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                            eventContext.getEvent(), getAddress().toString(), e.getMessage()), e);
                    contractException = e;
                    byte[] data = JSONSerializeUtils.serializeToJSON(Result.error()).getBytes(StandardCharsets.UTF_8);
                    contractMsg.set(data);
                    argv.set(0, data.length);
                }
                return argv;
            }, Arrays.asList(Type.I32, Type.I32), Collections.singletonList(Type.I32)), new Imports.Spec("sys_msg", argv -> {
                byte[] bytes = contractMsg.get();
                Memory memory = arInstance.get().exports.getMemory("memory");
                ByteBuffer mbf = memory.buffer();
                mbf.position(argv.get(0).intValue());
                mbf.put(bytes);
                return argv;
            }, Arrays.asList(Type.I32, Type.I32), Collections.singletonList(Type.I32))), module);
            Instance instance = module.instantiate(imports);
            arInstance.set(instance);

            return instance;
        } catch (Exception e) {
            throw new ContractExecuteException();
        } finally {
            module.close();
        }
    }

    @Override
    protected void beforeEvent(Object contractInstance, ContractEventContext eventContext) {
        contractException = null;
        Instance instance = (Instance) contractInstance;
        Function beforeEvent = instance.exports.getFunction("before_event");
        if (null != beforeEvent) {
            beforeEvent.apply();
        }
    }

    @Override
    protected BytesValue doProcessEvent(Object contractInstance, ContractEventContext eventContext) {
        Instance instance = (Instance) contractInstance;
        Function event = instance.exports.getFunction(eventContext.getEvent());
        if (null == event) {
            throw new ContractMethodNotFoundException();
        }
        BytesValue[] values = eventContext.getArgs().getValues();
        Memory memory = instance.exports.getMemory("memory");
        Object[] args = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            byte[] bytes = values[i].getBytes().toBytes();
            switch (values[i].getType()) {
                case TEXT:
                    Integer ptr = (Integer) instance.exports.getFunction("allocate").apply(bytes.length)[0];
                    ByteBuffer mbf = memory.buffer();
                    mbf.position(ptr);
                    mbf.put(bytes);
                    args[i] = ptr;
                    break;
                case INT32:
                    args[i] = BytesUtils.toInt(bytes);
                    break;
                case INT64:
                    args[i] = BytesUtils.toLong(bytes);
                    break;
                default:
                    throw new ContractParameterErrorException();
            }
        }
        Object[] results = event.apply(args);
        if (null != results && results.length > 0) {
            Object o1 = results[0];
            if (o1.getClass() == int.class || o1.getClass() == Integer.class) {
                Integer retPtr = (Integer) o1;
                memory = instance.exports.getMemory("memory");
                ByteBuffer mbf = memory.buffer();
                String ret = getString(retPtr, mbf);
                instance.exports.getFunction("drop_string").apply(retPtr);
                if (null != ret && ret.length() > 0) {
                    return TypedValue.fromText(ret);
                }
            } else {
                return TypedValue.fromInt64((long) o1);
            }
        }

        return null;
    }

    @Override
    protected void postEvent(Object contractInstance, ContractEventContext eventContext, LedgerException error) {
        Instance instance = (Instance) contractInstance;
        try {
            Function postEvent = instance.exports.getFunction("post_event");
            if (null != postEvent) {
                // 传递错误信息
                postEvent.apply((null == error && null == contractException) ? Result.SUCCESS : Result.ERROR);
            }
        } finally {
            instance.close();
        }
    }

    @Override
    protected void enableSecurityManager() {
    }

    @Override
    protected void disableSecurityManager() {
    }

    /**
     * Wasm合约与账本数据库交互
     *
     * @param eventContext
     * @param request
     * @return
     */
    private Result sysCall(ContractEventContext eventContext, String request) {
        Request req = JSONSerializeUtils.deserializeFromJSON(request, Request.class);
        switch (RequestType.valueOf(req.getRequestType())) {
            case LOG:
                return log(JSONSerializeUtils.deserializeFromJSON(request, LogRequest.class));
            case BEFORE_EVENT:
            case POST_EVENT:
                return Result.success();
            case GET_LEDGER_HASH:
                return new GetLedgerHashResult(eventContext.getCurrentLedgerHash().toString());
            case GET_CONTRACT_ADDRESS:
                return new GetContractAddressResult(eventContext.getCurrentContractAddress().toString());
            case GET_TX_HASH:
                return new GetTxHashResult(eventContext.getTransactionRequest().getTransactionHash().toBase58());
            case GET_TX_TIME:
                return new GetTxTimeResult(eventContext.getTransactionRequest().getTransactionContent().getTimestamp());
            case GET_SIGNERS:
                return getSigners(eventContext);
            case REGISTER_USER:
                return registerUser(eventContext, JSONSerializeUtils.deserializeFromJSON(request, RegisterUserRequest.class));
            case GET_USER:
                return getUser(eventContext, JSONSerializeUtils.deserializeFromJSON(request, GetUserRequest.class));
            case REGISTER_DATA_ACCOUNT:
                return registerDataAccount(eventContext, JSONSerializeUtils.deserializeFromJSON(request, RegisterDataAccountRequest.class));
            case GET_DATA_ACCOUNT:
                return getDataAccount(eventContext, JSONSerializeUtils.deserializeFromJSON(request, GetDataAccountRequest.class));
            case SET_TEXT:
                return setText(eventContext, JSONSerializeUtils.deserializeFromJSON(request, SetTextRequest.class));
            case SET_TEXT_WITH_VERSION:
                return setTextWithVersion(eventContext, JSONSerializeUtils.deserializeFromJSON(request, SetTextWithVersionRequest.class));
            case SET_INT64:
                return setInt64(eventContext, JSONSerializeUtils.deserializeFromJSON(request, SetInt64Request.class));
            case SET_INT64_WITH_VERSION:
                return setInt64WithVersion(eventContext, JSONSerializeUtils.deserializeFromJSON(request, SetInt64WithVersionRequest.class));
            case GET_VALUE_VERSION:
                return getValueVersion(eventContext, JSONSerializeUtils.deserializeFromJSON(request, GetValueVersionRequest.class));
            case GET_VALUE:
                return getValue(eventContext, JSONSerializeUtils.deserializeFromJSON(request, GetValueRequest.class));
            default:
                return Result.error();
        }
    }

    private Result log(LogRequest request) {
        switch (request.getLevel()) {
            case LogRequest.DEBUG:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(request.getMsg());
                }
                break;
            case LogRequest.INFO:
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(request.getMsg());
                }
                break;
            default:
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(request.getMsg());
                }
        }
        return Result.success();
    }

    private Result getValue(ContractEventContext eventContext, GetValueRequest request) {
        TypedKVEntry dataEntry;
        if (request.getVersion() == -1) {
            dataEntry = eventContext.getUncommittedLedger().getDataEntries(request.getAddress(), request.getKey())[0];
        } else {
            dataEntry = eventContext.getUncommittedLedger().getDataEntry(request.getAddress(), request.getKey(), request.getVersion());
        }
        if (dataEntry.getVersion() != -1) {
            return new GetValueResult(request.getKey(), dataEntry.getValue().toString(), dataEntry.getType().name(), dataEntry.getVersion());
        } else {
            return new GetValueResult(request.getKey(), "", "", dataEntry.getVersion());
        }
    }

    private Result getValueVersion(ContractEventContext eventContext, GetValueVersionRequest request) {
        TypedKVEntry dataEntry = eventContext.getUncommittedLedger().getDataEntries(request.getAddress(), request.getKey())[0];
        return new GetValueVersionResult(dataEntry.getVersion());
    }

    private Result setText(ContractEventContext eventContext, SetTextRequest request) {
        TypedKVEntry dataEntry = eventContext.getUncommittedLedger().getDataEntries(request.getAddress(), request.getKey())[0];
        eventContext.getLedger().dataAccount(request.getAddress()).setText(request.getKey(), request.getValue(), dataEntry.getVersion());
        return new SetKVResult(dataEntry.getVersion() + 1);
    }

    private Result setTextWithVersion(ContractEventContext eventContext, SetTextWithVersionRequest request) {
        eventContext.getLedger().dataAccount(request.getAddress()).setText(request.getKey(), request.getValue(), request.getVersion());
        return new SetKVResult(request.getVersion() + 1);
    }

    private Result setInt64(ContractEventContext eventContext, SetInt64Request request) {
        TypedKVEntry dataEntry = eventContext.getUncommittedLedger().getDataEntries(request.getAddress(), request.getKey())[0];
        eventContext.getLedger().dataAccount(request.getAddress()).setInt64(request.getKey(), request.getValue(), dataEntry.getVersion());
        return new SetKVResult(dataEntry.getVersion() + 1);
    }

    private Result setInt64WithVersion(ContractEventContext eventContext, SetInt64WithVersionRequest request) {
        eventContext.getLedger().dataAccount(request.getAddress()).setInt64(request.getKey(), request.getValue(), request.getVersion());
        return new SetKVResult(request.getVersion() + 1);
    }

    private GetSignersResult getSigners(ContractEventContext eventContext) {
        Set<BlockchainIdentity> txSigners = eventContext.getTxSigners();
        String[] signers = new String[txSigners.size()];
        int i = 0;
        for (BlockchainIdentity id : txSigners) {
            signers[i] = id.getAddress().toBase58();
            i++;
        }

        return new GetSignersResult(signers);
    }

    private RegisterUserResult registerUser(ContractEventContext eventContext, RegisterUserRequest request) {
        CryptoAlgorithm algorithm = Crypto.getAlgorithm(request.getAlgorithm());
        SignatureFunction signFunc = Crypto.getSignatureFunction(algorithm);
        AsymmetricKeypair cryptoKeyPair = signFunc.generateKeypair(request.getSeed().getBytes());
        BlockchainKeypair keypair = new BlockchainKeypair(cryptoKeyPair.getPubKey(), cryptoKeyPair.getPrivKey());
        eventContext.getLedger().users().register(keypair.getIdentity());

        return new RegisterUserResult(keypair.getAddress().toBase58());
    }

    private GetUserResult getUser(ContractEventContext eventContext, GetUserRequest request) {
        UserInfo user = eventContext.getLedger().getUser(request.getAddress());
        if (null != user) {
            return new GetUserResult(request.getAddress(), user.getPubKey().toBase58());
        } else {
            return new GetUserResult(request.getAddress());
        }
    }

    private RegisterDataAccountResult registerDataAccount(ContractEventContext eventContext, RegisterDataAccountRequest request) {
        CryptoAlgorithm algorithm = Crypto.getAlgorithm(request.getAlgorithm());
        SignatureFunction signFunc = Crypto.getSignatureFunction(algorithm);
        AsymmetricKeypair cryptoKeyPair = signFunc.generateKeypair(request.getSeed().getBytes());
        BlockchainKeypair keypair = new BlockchainKeypair(cryptoKeyPair.getPubKey(), cryptoKeyPair.getPrivKey());
        eventContext.getLedger().dataAccounts().register(keypair.getIdentity());

        return new RegisterDataAccountResult(keypair.getAddress().toBase58());
    }

    private GetDataAccountResult getDataAccount(ContractEventContext eventContext, GetDataAccountRequest request) {
        DataAccountInfo dataAccount = eventContext.getLedger().getDataAccount(request.getAddress());
        if (null != dataAccount) {
            return new GetDataAccountResult(request.getAddress(), dataAccount.getPubKey().toBase58());
        } else {
            return new GetDataAccountResult(request.getAddress());
        }
    }

    private String getString(Integer ptr, ByteBuffer mbf) {
        int count = 0;
        for (int i = ptr, max = mbf.limit(); i < max; i++) {
            mbf.position(i);
            byte b = mbf.get();
            if (b == 0) {
                break;
            }
            count++;
        }
        byte[] reqData = new byte[count];
        mbf.position(ptr);
        mbf.get(reqData);
        return new String(reqData);
    }
}
