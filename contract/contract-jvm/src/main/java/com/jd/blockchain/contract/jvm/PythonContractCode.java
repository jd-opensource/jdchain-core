package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.TypedValue;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import utils.Bytes;
import utils.io.BytesUtils;

/**
 * 基于 Python 源码加载合约；
 */
public class PythonContractCode extends AbstractContractCode {
    private static final String LANG = "python";

    private static ThreadLocal<ClassLoader> contractClassLoader = new ThreadLocal<>();
    private byte[] chainCode;

    public PythonContractCode(Bytes address, long version, byte[] chainCode) {
        super(address, version);
        this.chainCode = chainCode;

    }

    protected Object getContractInstance(ContractEventContext eventContext) {
        // 设置类加载器，适配GraalVM
        contractClassLoader.set(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(Context.class.getClassLoader());

        return Context.newBuilder(LANG).allowAllAccess(true).allowHostAccess(HostAccess.ALL).build();
    }

    @Override
    protected void beforeEvent(Object contractInstance, ContractEventContext eventContext) {
        Context context = (Context) contractInstance;
        context.eval(LANG, BytesUtils.toString(chainCode));
        Value contextBindings = context.getBindings(LANG);
        // 传递合约上下文
        contextBindings.putMember("eventContext", eventContext);
        // 传递日志组件
        contextBindings.putMember("LOGGER", LOGGER);
        // 执行 beforeEvent 方法
        if (contextBindings.hasMember("beforeEvent")) {
            context.eval(LANG, "beforeEvent(eventContext)");
        }
    }

    @Override
    protected BytesValue doProcessEvent(Object contractInstance, ContractEventContext eventContext) {
        Context context = (Context) contractInstance;
        // 参数解析，仅支持 String/int/long/boolean/byte[]
        BytesValue[] values = eventContext.getArgs().getValues();
        Object[] args = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            switch (values[i].getType()) {
                case TEXT:
                    args[i] = BytesUtils.toString(values[i].getBytes().toBytes());
                    break;
                case INT32:
                    args[i] = BytesUtils.toInt(values[i].getBytes().toBytes());
                    break;
                case INT64:
                    args[i] = BytesUtils.toLong(values[i].getBytes().toBytes());
                    break;
                case BOOLEAN:
                    args[i] = BytesUtils.toBoolean(values[i].getBytes().toBytes()[0]);
                    break;
                default:
                    args[i] = values[i].getBytes().toBytes();
                    break;
            }
        }
        context.getBindings(LANG).putMember("args", args);
        Value result = context.eval(LANG, eventContext.getEvent() + "(*args)");
        // 解析返回值，仅支持String/int/long/boolean/byte[]
        if (result.isString()) {// String
            return TypedValue.fromText(result.asString());
        } else if (result.isNumber() && result.fitsInLong()) { // long
            return TypedValue.fromInt64(result.asLong());
        } else if (result.isNumber() && result.fitsInInt()) { // int
            return TypedValue.fromInt32(result.asInt());
        } else if (result.isBoolean()) {
            return TypedValue.fromBoolean(result.asBoolean()); // boolean
        } else if (result.hasArrayElements()) {
            return TypedValue.fromBytes(result.as(byte[].class)); // byte[]
        } else {
            return null;
        }
    }

    @Override
    protected void postEvent(Object contractInstance, ContractEventContext eventContext, LedgerException error) {
        try {
            if (null == contractInstance) {
                return;
            }
            try (Context context = (Context) contractInstance) {
                Value contextBindings = context.getBindings(LANG);
                contextBindings.putMember("error", error);
                // 执行 beforeEvent 方法
                if (contextBindings.hasMember("postEvent")) {
                    context.eval(LANG, "postEvent(eventContext, error)");
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contractClassLoader.get());
        }
    }

    @Override
    protected void enableSecurityManager() {
        // Using graalvm supported access controlling
    }

    @Override
    protected void disableSecurityManager() {
        // Using graalvm supported access controlling
    }
}
