package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 写KV，long类型
 */
public class SetInt64WithVersionRequest extends SetInt64Request {

    @JSONField(name = "ver")
    private long version;

    public SetInt64WithVersionRequest(String address, String key, long value, long version) {
        super(address, key, value);
        setRequestType(RequestType.SET_INT64_WITH_VERSION.getCode());
        this.version = version;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
