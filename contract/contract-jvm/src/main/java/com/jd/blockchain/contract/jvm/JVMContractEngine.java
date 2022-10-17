package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.contract.engine.ContractEngine;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.ContractLang;
import utils.Bytes;

import java.util.HashMap;
import java.util.Map;

public class JVMContractEngine implements ContractEngine {

    private static final Map<ContractLang, ContractCodeLoader> loaders = new HashMap<>();

    static {
        loaders.put(ContractLang.Java, new JavaContractCodeLoader());
        loaders.put(ContractLang.JavaScript, new JavaScriptContractCodeLoader());
        loaders.put(ContractLang.Python, new PythonContractCodeLoader());
        loaders.put(ContractLang.Rust, new RustContractCodeLoader());
    }

    @Override
    public ContractCode setupContract(ContractInfo contractInfo) {
        Bytes address = contractInfo.getAddress();
        long version = contractInfo.getChainCodeVersion();
        return loaders
                .get(contractInfo.getLang())
                .loadContract(address, version, contractInfo.getChainCode());
    }
}
