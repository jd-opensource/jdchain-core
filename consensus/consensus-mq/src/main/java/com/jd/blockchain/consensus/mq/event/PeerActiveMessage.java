package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendType;
import com.jd.blockchain.consensus.mq.event.binaryproto.PeerActiveEvent;

public class PeerActiveMessage extends UnOrderMessage implements PeerActiveEvent {

  private String address;

  public PeerActiveMessage(ExtendType type, String address) {
    super(type);
    this.address = address;
  }

  @Override
  public String getAddress() {
    return address;
  }
}
