package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.ledger.ContractRuntimeConfig;
import com.jd.blockchain.ledger.LedgerInitProperties;

public class JVMContractRuntimeConfig implements ContractRuntimeConfig {

    // 合约执行超时时间，毫秒
    private long timeout;
    // 最大合约调用栈深
    private int maxStackDepth;

    public JVMContractRuntimeConfig() {
        this(LedgerInitProperties.DEFAULT_CONTRACT_TIMEOUT, LedgerInitProperties.DEFAULT_MAX_STACK_DEPTH);
    }
    
    public JVMContractRuntimeConfig(long timeout, int maxStackDepth) {
        this.timeout = timeout;
        this.maxStackDepth = maxStackDepth <= 0 ? LedgerInitProperties.DEFAULT_MAX_STACK_DEPTH : maxStackDepth;
    }

    public JVMContractRuntimeConfig(ContractRuntimeConfig contractRuntimeConfig) {
        this(contractRuntimeConfig.getTimeout(), contractRuntimeConfig.getMaxStackDepth());
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public int getMaxStackDepth() {
        return maxStackDepth;
    }
}
