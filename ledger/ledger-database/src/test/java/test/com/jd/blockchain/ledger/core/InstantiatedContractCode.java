package test.com.jd.blockchain.ledger.core;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.ContractException;
import com.jd.blockchain.contract.ContractType;
import com.jd.blockchain.contract.EventProcessingAware;
import com.jd.blockchain.contract.jvm.AbstractContractCode;
import com.jd.blockchain.contract.jvm.ContractDefinition;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.BytesValueEncoding;
import com.jd.blockchain.ledger.BytesValueList;
import com.jd.blockchain.ledger.LedgerException;
import org.springframework.util.ReflectionUtils;
import utils.Bytes;

import java.lang.reflect.Method;

public class InstantiatedContractCode<T> extends AbstractContractCode {

    private T instance;
    private ContractDefinition contractDefinition;

    public InstantiatedContractCode(Bytes address, long version, Class<T> delaredInterface, T instance) {
        super(address, version);
        this.instance = instance;
        this.contractDefinition = resolveContractDefinition(delaredInterface, instance.getClass());
    }

    private static ContractDefinition resolveContractDefinition(Class<?> declaredIntf, Class<?> implementedClass) {
        ContractType contractType = ContractType.resolve(declaredIntf);
        return new ContractDefinition(contractType, implementedClass);
    }

    @Override
    protected T getContractInstance() {
        return instance;
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

}