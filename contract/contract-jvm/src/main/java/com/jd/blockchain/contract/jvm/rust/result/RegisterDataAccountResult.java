package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 注册数据账户返回用户地址
 */
public class RegisterDataAccountResult extends Result {

    @JSONField(name = "a")
    private String address;

    public RegisterDataAccountResult(String address) {
        super(SUCCESS);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
