package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取数据版本
 */
public class GetValueVersionRequest extends Request {

    @JSONField(name = "a")
    private String address;
    @JSONField(name = "k")
    private String key;

    public GetValueVersionRequest(String address, String key) {
        super(RequestType.GET_VALUE_VERSION.getCode());
        this.address = address;
        this.key = key;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
