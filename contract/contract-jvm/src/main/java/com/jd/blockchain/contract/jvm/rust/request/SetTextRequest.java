package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 写KV，字符类型，不带版本
 */
public class SetTextRequest extends Request {

    @JSONField(name = "a")
    private String address;
    @JSONField(name = "k")
    private String key;
    @JSONField(name = "v")
    private String value;

    public SetTextRequest(String address, String key, String value) {
        super(RequestType.SET_TEXT.getCode());
        this.address = address;
        this.key = key;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
