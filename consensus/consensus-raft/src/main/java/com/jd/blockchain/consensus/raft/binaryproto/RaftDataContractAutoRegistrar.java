package com.jd.blockchain.consensus.raft.binaryproto;

import com.jd.binaryproto.DataContractAutoRegistrar;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.configure.ConsensusDataContractAutoRegistrar;
import com.jd.blockchain.consensus.raft.client.RaftSessionCredential;
import com.jd.blockchain.consensus.raft.settings.*;

public class RaftDataContractAutoRegistrar implements DataContractAutoRegistrar {

    @Override
    public int order() {
        return ConsensusDataContractAutoRegistrar.ORDER + 1;
    }

    @Override
    public void initContext(DataContractRegistry registry) {
        DataContractRegistry.register(RaftSettings.class);
        DataContractRegistry.register(RaftNodeSettings.class);
        DataContractRegistry.register(RaftNetworkSettings.class);
        DataContractRegistry.register(RaftConsensusSettings.class);
        DataContractRegistry.register(RaftClientSettings.class);
        DataContractRegistry.register(RaftClientIncomingSettings.class);
        DataContractRegistry.register(RaftSessionCredential.class);
    }

}
