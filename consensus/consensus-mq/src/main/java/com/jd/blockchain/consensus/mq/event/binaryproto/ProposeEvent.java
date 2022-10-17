package com.jd.blockchain.consensus.mq.event.binaryproto;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_MQ_PROPOSE)
public interface ProposeEvent extends MQEvent {

  @DataField(order = 0, primitiveType = PrimitiveType.INT32)
  int getProposer();

  @DataField(order = 1, primitiveType = PrimitiveType.INT64)
  long getTimestamp();

  @DataField(order = 2, primitiveType = PrimitiveType.INT64)
  long getLatestHeight();

  @DataField(order = 3, primitiveType = PrimitiveType.BYTES)
  byte[] getLatestHash();

  @DataField(order = 4, refContract = true, list = true)
  TxEvent[] getTxs();
}
