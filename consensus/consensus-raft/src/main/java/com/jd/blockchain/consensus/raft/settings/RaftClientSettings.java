package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_RAFT_CLI_SETTINGS)
public interface RaftClientSettings extends ClientSettings {

    @DataField(order = 1, list = true, primitiveType = PrimitiveType.TEXT)
    String[] getCurrentPeers();
}