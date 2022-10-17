package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendType;
import com.jd.blockchain.consensus.mq.event.binaryproto.UnOrderEvent;

public class UnOrderMessage implements UnOrderEvent {

  private ExtendType type;

  public UnOrderMessage(ExtendType type) {
    this.type = type;
  }

  public void setType(ExtendType type) {
    this.type = type;
  }

  @Override
  public ExtendType getType() {
    return type;
  }
}
