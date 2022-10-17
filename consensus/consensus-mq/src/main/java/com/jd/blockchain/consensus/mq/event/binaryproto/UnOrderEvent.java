package com.jd.blockchain.consensus.mq.event.binaryproto;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_MQ_MSG_UNORDER)
public interface UnOrderEvent {

  @DataField(order = 0, refEnum = true)
  ExtendType getType();
}
