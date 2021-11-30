
package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.PubKey;


@DataContract(code = DataCodes.CONSENSUS_RAFT_CLI_INCOMING_SETTINGS)
public interface RaftClientIncomingSettings extends ClientIncomingSettings {

    @DataField(order = 1, primitiveType = PrimitiveType.BYTES)
    PubKey getPubKey();

    @DataField(order = 2, list = true, primitiveType = PrimitiveType.TEXT)
    String[] getCurrentPeers();

}