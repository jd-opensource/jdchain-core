package com.jd.blockchain.consensus.mq.event.binaryproto;

import com.jd.binaryproto.EnumContract;
import com.jd.binaryproto.EnumField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

@EnumContract(code = DataCodes.CONSENSUS_MQ_MSG_TYPE)
public enum ExtendType {
    // 节点移除
    PING((byte) 0),
    // 节点变更
    PEER_ACTIVE((byte) 1),
    // 节点移除
    PEER_INACTIVE((byte) 2);

    @EnumField(type = PrimitiveType.INT8)
    public final byte CODE;

    ExtendType(byte code) {
        this.CODE = code;
    }
}
