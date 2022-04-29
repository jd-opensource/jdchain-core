package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 写KV，long类型，不带版本
 */
public class SetInt64Request extends Request {

    @JSONField(name = "a")
    private String address;
    @JSONField(name = "k")
    private String key;
    @JSONField(name = "v")
    private long value;

    public SetInt64Request(String address, String key, long value) {
        super(RequestType.SET_INT64.getCode());
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

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
