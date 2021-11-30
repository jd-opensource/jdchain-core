package com.jd.blockchain.consensus.raft.settings;

import com.alipay.sofa.jraft.option.RaftOptions;
import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_RAFT_SETTINGS_INFO)
public interface RaftSettings {

    @DataField(order = 1, primitiveType = PrimitiveType.INT32)
    int getMaxByteCountPerRpc();

    @DataField(order = 2, primitiveType = PrimitiveType.INT32)
    int getMaxEntriesSize();

    @DataField(order = 3, primitiveType = PrimitiveType.INT32)
    int getMaxBodySize();

    @DataField(order = 4, primitiveType = PrimitiveType.INT32)
    int getMaxAppendBufferSize();

    @DataField(order = 5, primitiveType = PrimitiveType.INT32)
    int getMaxElectionDelayMs();

    @DataField(order = 6, primitiveType = PrimitiveType.INT32)
    int getElectionHeartbeatFactor();

    @DataField(order = 7, primitiveType = PrimitiveType.INT32)
    int getApplyBatch();

    @DataField(order = 8, primitiveType = PrimitiveType.BOOLEAN)
    boolean isSync();

    @DataField(order = 9, primitiveType = PrimitiveType.BOOLEAN)
    boolean isSyncMeta();

    @DataField(order = 10, primitiveType = PrimitiveType.BOOLEAN)
    boolean isReplicatorPipeline();

    @DataField(order = 11, primitiveType = PrimitiveType.INT32)
    int getMaxReplicatorInflightMsgs();

    @DataField(order = 12, primitiveType = PrimitiveType.INT32)
    int getDisruptorBufferSize();

}
