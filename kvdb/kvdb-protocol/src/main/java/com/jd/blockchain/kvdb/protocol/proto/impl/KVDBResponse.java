package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.proto.Response;
import com.jd.blockchain.utils.Bytes;

public class KVDBResponse implements Response {

    private int code;
    private Bytes[] result;

    public KVDBResponse(int code) {
        this.code = code;
    }

    public KVDBResponse(int code, Bytes... result) {
        this.code = code;
        this.result = result;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setResult(Bytes... result) {
        this.result = result;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public Bytes[] getResult() {
        return result;
    }

}
