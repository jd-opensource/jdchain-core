package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Bytes;

import java.util.concurrent.*;

/**
 * @author huanghaiquan
 */
public abstract class AbstractContractCode implements ContractCode {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractContractCode.class);
    private Bytes address;
    private long version;

    public AbstractContractCode(Bytes address, long version) {
        this.address = address;
        this.version = version;
    }

    @Override
    public Bytes getAddress() {
        return address;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public BytesValue processEvent(ContractEventContext eventContext) {
        Object retn = null;
        LedgerException error = null;
        Object contractInstance = null;
        try {
            contractInstance = getContractInstance();
            // 执行预处理;
            beforeEvent(contractInstance, eventContext);

            ContractRuntimeConfig contractRuntimeConfig = eventContext.getContractRuntimeConfig();
            // 判断存在合约运行时配置
            if (null != contractRuntimeConfig) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    Object finalContractInstance = contractInstance;
                    Future<Object> future = executor.submit(() -> securityInvoke(eventContext, finalContractInstance));
                    retn = future.get(contractRuntimeConfig.getTimeout(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // 超时异常，区块回滚
                    throw new BlockRollbackException(TransactionState.TIMEOUT, "Contract timeout");
                } catch (Exception e) {
                    String errorMessage = String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                            eventContext.getEvent(), address.toString(), e.getMessage());
                    throw new ContractExecuteException(errorMessage, e);
                } finally {
                    executor.shutdown();
                }
            } else {
                retn = securityInvoke(eventContext, contractInstance);
            }

        } catch (LedgerException e) {
            error = e;
        } catch (Throwable e) {
            String errorMessage = String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                    eventContext.getEvent(), address.toString(), e.getMessage());
            error = new ContractExecuteException(errorMessage, e);
        }

        try {
            postEvent(contractInstance, eventContext, error);
        } catch (Throwable e) {
            throw new ContractExecuteException("Error occurred while posting contract event!", e);
        }
        if (error != null) {
            throw error;
        }
        return (BytesValue) retn;
    }

    private Object securityInvoke(ContractEventContext eventContext, Object contractInstance) {
        RuntimeSecurityManager securityManager = RuntimeContext.get().getSecurityManager();
        try {
            if (null != securityManager) {
                securityManager.enable();
            }
            return doProcessEvent(contractInstance, eventContext);
        } finally {
            if (null != securityManager) {
                securityManager.disable();
            }
        }
    }

    protected abstract Object getContractInstance();

    protected abstract void beforeEvent(Object contractInstance, ContractEventContext eventContext);

    protected abstract BytesValue doProcessEvent(Object contractInstance, ContractEventContext eventContext);

    protected abstract void postEvent(Object contractInstance, ContractEventContext eventContext, LedgerException error);
}
