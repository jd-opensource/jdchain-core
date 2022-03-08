package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.*;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.BytesValueEncoding;
import com.jd.blockchain.ledger.BytesValueList;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.runtime.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import utils.Bytes;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 基于 java jar 包并且以模块化方式独立加载的合约代码；
 *
 * @author huanghaiquan
 */
public class JavaContractCode extends AbstractContractCode {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaContractCode.class);
    private Module codeModule;
    private ContractDefinition contractDefinition;

    public JavaContractCode(Bytes address, long version, Module codeModule) {
        super(address, version);
        this.codeModule = codeModule;
        this.contractDefinition = resolveContractDefinition(codeModule);
    }

    protected static ContractDefinition resolveContractDefinition(Module codeModule) {
        String mainClassName = codeModule.getMainClass();
        Class<?> mainClass = codeModule.loadClass(mainClassName);
        Class<?>[] interfaces = mainClass.getInterfaces();
        Class<?> contractInterface = null;
        for (Class<?> itf : interfaces) {
            Contract annoContract = itf.getAnnotation(Contract.class);
            if (annoContract != null) {
                if (contractInterface == null) {
                    contractInterface = itf;
                } else {
                    throw new ContractException(
                            "One contract definition is only allowed to implement one contract type!");
                }
            }
        }
        if (contractInterface == null) {
            throw new ContractException("No contract type is implemented!");
        }
        ContractType type = ContractType.resolve(contractInterface);
        return new ContractDefinition(type, mainClass);
    }

    @Override
    public BytesValue processEvent(ContractEventContext eventContext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start processing event{} of contract{}...", eventContext.getEvent(), getAddress().toString());
        }
        try {
            return codeModule.call(new ContractExecution(eventContext));
        } catch (Exception ex) {
            LOGGER.error(String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                    eventContext.getEvent(), getAddress().toString(), ex.getMessage()), ex);
            throw ex;
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("End processing event{} of contract{}. ", eventContext.getEvent(), getAddress().toString());
            }
        }
    }

    protected Object getContractInstance() {
        try {
            // 每一次调用都通过反射创建合约的实例；
            return contractDefinition.getMainClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void beforeEvent(Object contractInstance, ContractEventContext eventContext) {
        if (contractInstance instanceof EventProcessingAware) {
            ((EventProcessingAware) contractInstance).beforeEvent(eventContext);
        }
    }

    @Override
    protected BytesValue doProcessEvent(Object contractInstance, ContractEventContext eventContext) {
        // 反序列化参数；
        Method handleMethod = contractDefinition.getType().getHandleMethod(eventContext.getEvent());

        if (handleMethod == null) {
            throw new ContractException(
                    String.format("Contract[%s:%s] has no handle method to handle event[%s]!", getAddress().toString(),
                            contractDefinition.getType().getName(), eventContext.getEvent()));
        }

        BytesValueList bytesValues = eventContext.getArgs();
        Object[] args = BytesValueEncoding.decode(bytesValues, handleMethod.getParameterTypes());

        return BytesValueEncoding.encodeSingle(ReflectionUtils.invokeMethod(handleMethod, contractInstance, args), handleMethod.getReturnType());
    }

    @Override
    protected void postEvent(Object contractInstance, ContractEventContext eventContext, LedgerException error) {
        if (contractInstance instanceof EventProcessingAware) {
            ((EventProcessingAware) contractInstance).postEvent(eventContext, error);
        }
    }

    private class ContractExecution implements Callable<BytesValue> {
        private ContractEventContext eventContext;

        public ContractExecution(ContractEventContext contractEventContext) {
            this.eventContext = contractEventContext;
        }

        @Override
        public BytesValue call() {
            return JavaContractCode.super.processEvent(eventContext);
        }
    }

}
