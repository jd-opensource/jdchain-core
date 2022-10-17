package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendType;
import com.jd.blockchain.consensus.mq.event.binaryproto.PeerInactiveEvent;

public class PeerInactiveMessage extends UnOrderMessage implements PeerInactiveEvent {

  private String address;

  public PeerInactiveMessage(ExtendType type, String address) {
    super(type);
    this.address = address;
  }

  @Override
  public String getAddress() {
    return address;
  }
}
