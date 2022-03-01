package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.runtime.Module;
import com.jd.blockchain.runtime.RuntimeContext;
import utils.Bytes;

public class JavaContractCodeLoader implements ContractCodeLoader {

    private RuntimeContext runtimeContext = RuntimeContext.get();

    private String getCodeName(Bytes address, long version) {
        return address.toBase58() + "_" + version;
    }

    @Override
    public ContractCode loadContract(Bytes address, long version, byte[] codeBytes) {
        String codeName = getCodeName(address, version);
        Module module = runtimeContext.createDynamicModule(codeName, codeBytes);
        if (module == null) {
            return null;
        }
        return new JavaContractCode(address, version, module);
    }
}
