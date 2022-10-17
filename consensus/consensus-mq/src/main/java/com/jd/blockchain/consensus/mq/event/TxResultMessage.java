package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.TxResult;

public class TxResultMessage implements TxResult {

  private String key;
  private byte[] result;

  public TxResultMessage(String key, byte[] result) {
    this.key = key;
    this.result = result;
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
