package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取交易签名用户列表
 */
public class GetSignersResult extends Result {

    @JSONField(name = "ss")
    private String[] signers;

    public GetSignersResult(String[] signers) {
        super(SUCCESS);
        this.signers = signers;
    }

    public String[] getSigners() {
        return signers;
    }

    public void setSigners(String[] signers) {
        this.signers = signers;
    }
}
