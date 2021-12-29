package com.jd.blockchain.consensus.mq.event;

import java.io.Serializable;

public class TxMessage implements Serializable {

    String key;
    byte[] message;

    public TxMessage(String key, byte[] message) {
        this.key = key;
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }
}