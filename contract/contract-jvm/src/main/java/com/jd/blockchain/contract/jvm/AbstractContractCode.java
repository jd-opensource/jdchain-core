package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.ContractException;
import com.jd.blockchain.contract.EventProcessingAware;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.runtime.RuntimeContext;
import com.jd.blockchain.runtime.RuntimeSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import utils.Bytes;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huanghaiquan
 */
public abstract class AbstractContractCode implements ContractCode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContractCode.class);
    private Bytes address;
    private long version;

    private ContractDefinition contractDefinition;

    public AbstractContractCode(Bytes address, long version, ContractDefinition contractDefinition) {
        this.address = address;
        this.version = version;
        this.contractDefinition = contractDefinition;
    }

    public ContractDefinition getContractDefinition() {
        return contractDefinition;
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
        EventProcessingAware evtProcAwire = null;
        Object retn = null;
        LedgerException error = null;
        Class<?> returnType = null;
        try {
            // 执行预处理;
            Object contractInstance = getContractInstance();
            if (contractInstance instanceof EventProcessingAware) {
                evtProcAwire = (EventProcessingAware) contractInstance;
            }

            if (evtProcAwire != null) {
                evtProcAwire.beforeEvent(eventContext);
            }

            // 反序列化参数；
            Method handleMethod = contractDefinition.getType().getHandleMethod(eventContext.getEvent());
            returnType = handleMethod.getReturnType();

            if (handleMethod == null) {
                throw new ContractException(
                        String.format("Contract[%s:%s] has no handle method to handle event[%s]!", address.toString(),
                                contractDefinition.getType().getName(), eventContext.getEvent()));
            }

            BytesValueList bytesValues = eventContext.getArgs();
            Object[] args;
            try {
                args = BytesValueEncoding.decode(bytesValues, handleMethod.getParameterTypes());
            } catch (Exception e) {
                throw new ContractException("Contract parameters wrong!", e);
            }
            ContractRuntimeConfig contractRuntimeConfig = eventContext.getContractRuntimeConfig();
            // 判断存在合约运行时配置
            if (null != contractRuntimeConfig) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    Future<Object> future = executor.submit(() -> securityInvoke(handleMethod, contractInstance, args));
                    retn = future.get(contractRuntimeConfig.getTimeout(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    String errorMessage = String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                            eventContext.getEvent(), address.toString(), e.getMessage());
                    throw new ContractExecuteException(errorMessage, e);
                } finally {
                    executor.shutdown();
                }
            } else {
                retn = securityInvoke(handleMethod, contractInstance, args);
            }
        } catch (LedgerException e) {
            error = e;
        } catch (Throwable e) {
            String errorMessage = String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                    eventContext.getEvent(), address.toString(), e.getMessage());
            error = new ContractExecuteException(errorMessage, e);
        }

        if (evtProcAwire != null) {
            try {
                evtProcAwire.postEvent(eventContext, error);
            } catch (Throwable e) {
                String errorMessage = "Error occurred while posting contract event! --" + e.getMessage();
                LOGGER.error(errorMessage, e);
                throw new ContractExecuteException(errorMessage);
            }
        }
        if (error != null) {
            // Rethrow error;
            throw error;
        }

        BytesValue retnBytes = BytesValueEncoding.encodeSingle(retn, returnType);
        return retnBytes;
    }

    private Object securityInvoke(Method handleMethod, Object contractInstance, Object[] args) {
        RuntimeSecurityManager securityManager = RuntimeContext.get().getSecurityManager();
        try {
            if (null != securityManager) {
                securityManager.enable();
            }
            return ReflectionUtils.invokeMethod(handleMethod, contractInstance, args);
        } finally {
            if (null != securityManager) {
                securityManager.disable();
            }
        }
    }

    protected abstract Object getContractInstance();

}
