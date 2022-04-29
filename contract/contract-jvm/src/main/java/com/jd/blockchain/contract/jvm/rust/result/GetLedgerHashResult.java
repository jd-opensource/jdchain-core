package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取账本哈希
 */
public class GetLedgerHashResult extends Result {

    @JSONField(name = "lh")
    private String ledgerHash;

    public GetLedgerHashResult(String ledgerHash) {
        super(SUCCESS);
        this.ledgerHash = ledgerHash;
    }

    public String getLedgerHash() {
        return ledgerHash;
    }

    public void setLedgerHash(String ledgerHash) {
        this.ledgerHash = ledgerHash;
    }
}
