package com.jd.blockchain.consensus.mq.consumer;

public interface MsgQueueHandler {

    void handle(byte[] msg);

}
