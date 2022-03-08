package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_RAFT_NETWORK_SETTINGS)
public interface RaftNetworkSettings {

    @DataField(order = 1, primitiveType = PrimitiveType.INT32)
    int getRpcConnectTimeoutMs();

    @DataField(order = 2, primitiveType = PrimitiveType.INT32)
    int getRpcDefaultTimeoutMs();

    @DataField(order = 3, primitiveType = PrimitiveType.INT32)
    int getRpcSnapshotTimeoutMs();

    @DataField(order = 4, primitiveType = PrimitiveType.INT32)
    int getRpcRequestTimeoutMs();

}
