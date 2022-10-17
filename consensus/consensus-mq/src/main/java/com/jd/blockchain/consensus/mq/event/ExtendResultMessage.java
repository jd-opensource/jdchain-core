package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendResult;
import utils.io.BytesUtils;

public class ExtendResultMessage implements ExtendResult {

    private String key;
    private byte[] result;

    public ExtendResultMessage(String key, byte[] result) {
        this.key = key;
        this.result = result;
    }

    public static ExtendResultMessage success(String key) {
        return new ExtendResultMessage(key, BytesUtils.toBytes(true));
    }

    public static ExtendResultMessage success(String key, byte[] result) {
        return new ExtendResultMessage(key, result);
    }

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }
}
