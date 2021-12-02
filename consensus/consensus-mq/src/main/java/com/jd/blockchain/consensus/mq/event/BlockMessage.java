package com.jd.blockchain.consensus.mq.event;

import java.util.List;

public class BlockMessage {

    private long height;
    private long timestamp;
    private byte[] hash;
    private List<TxMessage> txMessages;

    public BlockMessage(long height, long timestamp, byte[] hash, List<TxMessage> txMessages) {
        this.height = height;
        this.timestamp = timestamp;
        this.hash = hash;
        this.txMessages = txMessages;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public List<TxMessage> getTxMessages() {
        return txMessages;
    }

    public void setTxMessages(List<TxMessage> txMessages) {
        this.txMessages = txMessages;
    }
}
