package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 查询用户
 */
public class GetUserRequest extends Request {

    @JSONField(name = "a")
    private String address;

    public GetUserRequest(String address) {
        super(RequestType.GET_USER.getCode());
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
