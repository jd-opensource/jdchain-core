package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取交易时间
 */
public class GetTxTimeResult extends Result {

    @JSONField(name = "tt")
    private long txTime;

    public GetTxTimeResult(long txTime) {
        super(SUCCESS);
        this.txTime = txTime;
    }

    public long getTxTime() {
        return txTime;
    }

    public void setTxTime(long txTime) {
        this.txTime = txTime;
    }
}
