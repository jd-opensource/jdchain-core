package test.com.jd.blockchain.ledger.core;

import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.core.handles.ContractEventSendOperationHandle;
import utils.Bytes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContractInvokingHandle extends ContractEventSendOperationHandle {

    private Map<Bytes, ContractCode> contractInstances = new ConcurrentHashMap<Bytes, ContractCode>();

    public <T> ContractCode setup(Bytes address, Class<T> contractIntf, T instance) {
        InstantiatedContractCode<T> contract = new InstantiatedContractCode<T>(address, 0, contractIntf, instance);
        contractInstances.put(address, contract);
        return contract;
    }


}
