package com.jd.blockchain.consensus.mq.event;

import java.util.List;

public class BlockEvent {

    private long timestamp;
    private long height;
    private List<MessageEvent> messageEvents;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<MessageEvent> getMessageEvents() {
        return messageEvents;
    }

    public void setMessageEvents(List<MessageEvent> messageEvents) {
        this.messageEvents = messageEvents;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public BlockEvent(long height, long timestamp, List<MessageEvent> messageEvents) {
        this.height = height;
        this.timestamp = timestamp;
        this.messageEvents = messageEvents;
    }
}
