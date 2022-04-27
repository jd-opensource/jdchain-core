package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 查询数据账户
 */
public class GetDataAccountRequest extends Request {

    @JSONField(name = "a")
    private String address;

    public GetDataAccountRequest(String address) {
        super(RequestType.GET_DATA_ACCOUNT.getCode());
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
