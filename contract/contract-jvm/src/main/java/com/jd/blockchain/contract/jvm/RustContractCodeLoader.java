package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.engine.ContractCode;
import utils.Bytes;

public class RustContractCodeLoader implements ContractCodeLoader {

    @Override
    public ContractCode loadContract(Bytes address, long version, byte[] codeBytes) {
        return new RustContractCode(address, version, codeBytes);
    }
}
