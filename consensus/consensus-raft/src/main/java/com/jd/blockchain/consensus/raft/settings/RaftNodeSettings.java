package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consts.DataCodes;
import utils.net.NetworkAddress;

@DataContract(code = DataCodes.CONSENSUS_RAFT_NODE_SETTINGS)
public interface RaftNodeSettings extends NodeSettings {

    @DataField(order = 2, primitiveType = PrimitiveType.INT32)
    int getId();

    @DataField(order = 3, primitiveType = PrimitiveType.BYTES)
    NetworkAddress getNetworkAddress();

}