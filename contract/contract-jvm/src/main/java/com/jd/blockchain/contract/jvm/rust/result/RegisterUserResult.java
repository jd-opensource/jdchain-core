package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 注册用户返回用户地址
 */
public class RegisterUserResult extends Result {

    @JSONField(name = "a")
    private String address;

    public RegisterUserResult(String address) {
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
