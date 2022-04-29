package com.jd.blockchain.contract.jvm.rust;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Wasm调用账本数据库请求
 */
public class Request {

    /**
     * 请求类型
     */
    @JSONField(name = "rt")
    private int requestType;

    public Request(int requestType) {
        this.requestType = requestType;
    }

    public int getRequestType() {
        return requestType;
    }

    public void setRequestType(int requestType) {
        this.requestType = requestType;
    }
}
