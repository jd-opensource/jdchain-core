package com.jd.blockchain.consensus.mq.event;


import java.io.Serializable;

public class ResultMessage implements Serializable {

    private String key;
    private MessageType type;
    private byte[] result;

    public ResultMessage(String key, MessageType type, byte[] result) {
        this.key = key;
        this.type = type;
        this.result = result;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }
}