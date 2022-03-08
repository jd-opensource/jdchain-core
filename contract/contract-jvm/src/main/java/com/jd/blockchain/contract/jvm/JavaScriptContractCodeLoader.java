package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.engine.ContractCode;
import utils.Bytes;

public class JavaScriptContractCodeLoader implements ContractCodeLoader {

    @Override
    public ContractCode loadContract(Bytes address, long version, byte[] codeBytes) {
        return new JavaScriptContractCode(address, version, codeBytes);
    }
}
