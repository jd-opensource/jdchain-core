package com.jd.blockchain.consensus.mq.consumer;

public interface MQHandler {

  void handle(byte[] msg);
}
