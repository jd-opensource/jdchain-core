package com.jd.blockchain.consensus.mq.event.binaryproto;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_MQ_MSG_PEER_ACTIVE)
public interface PeerActiveEvent extends UnOrderEvent {

  @DataField(order = 0, primitiveType = PrimitiveType.TEXT)
  String getAddress();
}
