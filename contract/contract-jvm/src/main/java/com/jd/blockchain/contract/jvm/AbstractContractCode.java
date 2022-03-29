package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.runtime.RuntimeContext;
import com.jd.blockchain.runtime.RuntimeSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Bytes;

import java.util.concurrent.*;

/**
 * @author huanghaiquan
 */
public abstract class AbstractContractCode implements ContractCode {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractContractCode.class);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
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
        ContractRuntimeConfig contractRuntimeConfig = eventContext.getContractRuntimeConfig();
        // 判断存在合约运行时配置
        if (null != contractRuntimeConfig) {
            return timeLimitedInvoke(eventContext, contractRuntimeConfig);
        } else {
            return invoke(eventContext);
        }
    }

    private BytesValue invoke(ContractEventContext eventContext) {
        Object retn = null;
        LedgerException error = null;
        Object contractInstance = null;
        try {
            try {
                // 生成合约类对象
                contractInstance = getContractInstance(eventContext);
                // 开启安全管理器
                enableSecurityManager();

                // 执行预处理;
                beforeEvent(contractInstance, eventContext);

                // 合约方法执行
                retn = doProcessEvent(contractInstance, eventContext);
            } catch (Throwable e) {
                if (e instanceof LedgerException) {
                    error = (LedgerException) e;
                } else if (e.getCause() instanceof LedgerException) {
                    error = (LedgerException) e.getCause();
                } else {
                    String errorMessage = String.format("Error occurred while processing event[%s] of contract[%s]!", eventContext.getEvent(), address.toString());
                    error = new ContractExecuteException(errorMessage, e);
                }
            }

            try {
                // 执行后处理
                postEvent(contractInstance, eventContext, error);
            } catch (Throwable e) {
                if (e instanceof LedgerException) {
                    error = (LedgerException) e;
                } else {
                    String errorMessage = String.format("Error occurred while posting event[%s] of contract[%s]!", eventContext.getEvent(), address.toString());
                    error = new ContractExecuteException(errorMessage, e);
                }
            }
            if (error != null) {
                throw error;
            }
            return (BytesValue) retn;
        } finally {
            disableSecurityManager();
        }
    }

    private BytesValue timeLimitedInvoke(ContractEventContext eventContext, ContractRuntimeConfig runtimeConfig) {
        try {
            SecurityPolicy usersPolicy = SecurityContext.getContextUsersPolicy();
            Future<Object> future = executor.submit(() -> {
                SecurityContext.setContextUsersPolicy(usersPolicy);
                return invoke(eventContext);
            });
            return (BytesValue) future.get(runtimeConfig.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof LedgerException) {
                throw (LedgerException) e.getCause();
            }
            throw new ContractExecuteException("Error occurred while get contract result ", e);
        } catch (InterruptedException | TimeoutException e) {
            throw new BlockRollbackException(TransactionState.SYSTEM_ERROR, "Contract Interrupted or Timeout", e);
        } catch (Exception e) {
            throw new BlockRollbackException(TransactionState.SYSTEM_ERROR, "Contract Interrupted or Timeout", e);
        }
    }

    protected void enableSecurityManager() {
        RuntimeSecurityManager securityManager = RuntimeContext.get().getSecurityManager();
        if (null != securityManager) {
            securityManager.enable();
        }
    }

    protected void disableSecurityManager() {
        RuntimeSecurityManager securityManager = RuntimeContext.get().getSecurityManager();
        if (null != securityManager) {
            securityManager.disable();
        }
    }

    protected abstract Object getContractInstance(ContractEventContext eventContext);

    protected abstract void beforeEvent(Object contractInstance, ContractEventContext eventContext);

    protected abstract BytesValue doProcessEvent(Object contractInstance, ContractEventContext eventContext) throws Exception;

    protected abstract void postEvent(Object contractInstance, ContractEventContext eventContext, LedgerException error);
}
