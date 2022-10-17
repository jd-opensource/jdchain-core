package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendType;
import com.jd.blockchain.consensus.mq.event.binaryproto.PingEvent;

public class PingMessage extends UnOrderMessage implements PingEvent {

  private String address;
  private NodeNetworkAddress nodeNetwork;

  public PingMessage(ExtendType type, String address, NodeNetworkAddress nodeNetwork) {
    super(type);
    this.address = address;
    this.nodeNetwork = nodeNetwork;
  }

  @Override
  public String getAddress() {
    return address;
  }

  @Override
  public NodeNetworkAddress getNodeNetwork() {
    return nodeNetwork;
  }
}
