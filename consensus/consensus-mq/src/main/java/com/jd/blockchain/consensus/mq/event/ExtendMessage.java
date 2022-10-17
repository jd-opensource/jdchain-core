package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendEvent;

public class ExtendMessage implements ExtendEvent {

  private String key;
  private byte[] message;

  public ExtendMessage(String key, byte[] message) {
    this.key = key;
    this.message = message;
  }

  @Override
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public byte[] getMessage() {
    return message;
  }

  public void setMessage(byte[] message) {
    this.message = message;
  }
}
