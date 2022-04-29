package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * KV数据版本返回
 */
public class GetValueVersionResult extends Result {

    @JSONField(name = "ver")
    private long version;

    public GetValueVersionResult(long version) {
        super(SUCCESS);
        this.version = version;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
