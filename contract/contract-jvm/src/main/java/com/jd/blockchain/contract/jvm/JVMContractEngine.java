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
    // TODO imuge LRU
    private Map<String, ContractCode> contracts = new HashMap<>();

    static {
        loaders.put(ContractLang.Java, new JavaContractCodeLoader());
        loaders.put(ContractLang.JavaScript, new JavaScriptContractCodeLoader());
        loaders.put(ContractLang.Python, new PythonContractCodeLoader());
        loaders.put(ContractLang.Rust, new RustContractCodeLoader());
    }

    private String getCodeName(Bytes address, long version) {
        return address.toBase58() + "_" + version;
    }

    @Override
    public ContractCode setupContract(ContractInfo contractInfo) {
        Bytes address = contractInfo.getAddress();
        long version = contractInfo.getChainCodeVersion();
        String codeName = getCodeName(address, version);
        ContractCode contractCode = contracts.get(version);
        if (null == contractCode) {
            synchronized (JVMContractEngine.class) {
                contractCode = contracts.get(version);
                if (null == contractCode) {
                    contractCode = loaders.get(contractInfo.getLang()).loadContract(address, version, contractInfo.getChainCode());
                    if (null != contractCode) {
                        contracts.put(codeName, contractCode);
                    }
                }
            }
        }

        return contractCode;
    }
}
