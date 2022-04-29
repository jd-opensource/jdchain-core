package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取交易哈希
 */
public class GetTxHashResult extends Result {

    @JSONField(name = "th")
    private String txHash;

    public GetTxHashResult(String txHash) {
        super(SUCCESS);
        this.txHash = txHash;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
