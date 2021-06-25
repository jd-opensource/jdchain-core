package com.jd.blockchain.gateway.service.search;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.TransactionState;

/**
 * 交易搜索结果
 */
public class Transaction {
    // 交易哈希
    private HashDigest hash;
    // 交易所在区块高度
    @JSONField(name = "block_height")
    private long blockHeight;
    // 交易执行状态
    @JSONField(name = "execution_state")
    private TransactionState executionState;

    public Transaction(HashDigest hash, long blockHeight, TransactionState executionState) {
        this.hash = hash;
        this.blockHeight = blockHeight;
        this.executionState = executionState;
    }

    public HashDigest getHash() {
        return hash;
    }

    public void setHash(HashDigest hash) {
        this.hash = hash;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public TransactionState getExecutionState() {
        return executionState;
    }

    public void setExecutionState(TransactionState executionState) {
        this.executionState = executionState;
    }
}
