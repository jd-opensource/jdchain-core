package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * KV数据
 */
public class GetValueResult extends Result {

    @JSONField(name = "k")
    private String key;
    @JSONField(name = "v")
    private String value;
    @JSONField(name = "t")
    private String type;
    @JSONField(name = "ver")
    private long version;

    public GetValueResult(String key, String value, String type, long version) {
        super(SUCCESS);
        this.key = key;
        this.value = value;
        this.type = type;
        this.version = version;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
