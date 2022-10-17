package com.jd.blockchain.consensus.mq.event.binaryproto;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_MQ_MSG_PING)
public interface PingEvent extends UnOrderEvent {

  @DataField(order = 0, primitiveType = PrimitiveType.TEXT)
  String getAddress();

  @DataField(order = 1, refContract = true)
  NodeNetworkAddress getNodeNetwork();
}
