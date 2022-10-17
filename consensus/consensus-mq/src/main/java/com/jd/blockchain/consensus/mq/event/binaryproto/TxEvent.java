package com.jd.blockchain.consensus.mq.event.binaryproto;


import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_MQ_TX)
public interface TxEvent extends MQEvent {

  @DataField(order = 0, primitiveType = PrimitiveType.TEXT)
  String getKey();

  @DataField(order = 1, primitiveType = PrimitiveType.BYTES)
  byte[] getMessage();

  static TxEvent[] newArray(int size) {
    return new TxEvent[size];
  }
}
