package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取数据
 */
public class GetValueRequest extends Request {

    @JSONField(name = "a")
    private String address;
    @JSONField(name = "k")
    private String key;
    @JSONField(name = "ver")
    private long version;

    public GetValueRequest(String address, String key, long version) {
        super(RequestType.GET_VALUE.getCode());
        this.address = address;
        this.key = key;
        this.version = version;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
