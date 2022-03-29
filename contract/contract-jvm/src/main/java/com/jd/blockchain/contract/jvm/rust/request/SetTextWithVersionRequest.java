package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 写KV，字符类型
 */
public class SetTextWithVersionRequest extends SetTextRequest {

    @JSONField(name = "ver")
    private long version;

    public SetTextWithVersionRequest(String address, String key, String value, long version) {
        super(address, key, value);
        setRequestType(RequestType.SET_TEXT_WITH_VERSION.getCode());
        this.version = version;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
