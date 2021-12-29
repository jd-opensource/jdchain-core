package com.jd.blockchain.consensus.mq.event;

import java.io.Serializable;

public class ExtendMessage implements Serializable {

    private MessageType type;
    private byte[] msg;

    public ExtendMessage(MessageType type, byte[] msg) {
        this.type = type;
        this.msg = msg;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public byte[] getMsg() {
        return msg;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }
}
