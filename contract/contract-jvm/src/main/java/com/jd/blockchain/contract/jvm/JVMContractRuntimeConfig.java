package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.ledger.ContractRuntimeConfig;

public class JVMContractRuntimeConfig implements ContractRuntimeConfig {

    // 合约执行超时时间，毫秒
    private long timeout;

    public JVMContractRuntimeConfig(long timeout) {
        this.timeout = timeout;
    }

    public JVMContractRuntimeConfig(ContractRuntimeConfig contractRuntimeConfig) {
        this.timeout = contractRuntimeConfig.getTimeout();
    }

    @Override
    public long getTimeout() {
        return timeout;
    }
}
