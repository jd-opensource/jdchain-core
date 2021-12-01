package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.service.StateSnapshot;

import java.util.List;

public class BlockEvent {

    private long height;
    private long timestamp;
    private byte[] hash;
    private List<MessageEvent> txEvents;

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

    public List<MessageEvent> getTxEvents() {
        return txEvents;
    }

    public void setTxEvents(List<MessageEvent> txEvents) {
        this.txEvents = txEvents;
    }

    public BlockEvent(long height, long timestamp, byte[] hash, List<MessageEvent> txEvents) {
        this.height = height;
        this.timestamp = timestamp;
        this.hash = hash;
        this.txEvents = txEvents;
    }
}
