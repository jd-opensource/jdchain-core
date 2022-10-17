package com.jd.blockchain.consensus.mq.binaryproto;

import com.jd.binaryproto.DataContractAutoRegistrar;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.configure.ConsensusDataContractAutoRegistrar;
import com.jd.blockchain.consensus.mq.client.MQCredentialInfo;
import com.jd.blockchain.consensus.mq.event.binaryproto.*;
import com.jd.blockchain.consensus.mq.settings.MQClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MQNodeSettings;

public class MQDataContractAutoRegistrar implements DataContractAutoRegistrar {

    @Override
    public int order() {
        return ConsensusDataContractAutoRegistrar.ORDER + 1;
    }

    @Override
    public void initContext(DataContractRegistry registry) {
        DataContractRegistry.register(MQConsensusSettings.class);
        DataContractRegistry.register(MQNodeSettings.class);
        DataContractRegistry.register(MQClientIncomingSettings.class);
        DataContractRegistry.register(MQCredentialInfo.class);

        DataContractRegistry.register(MQEvent.class);
        DataContractRegistry.register(TxEvent.class, TxEvent::newArray);
        DataContractRegistry.register(TxResult.class);
        DataContractRegistry.register(ProposeEvent.class);
        DataContractRegistry.register(ExtendEvent.class);
        DataContractRegistry.register(ExtendResult.class);

        DataContractRegistry.register(UnOrderEvent.class);
        DataContractRegistry.register(PingEvent.class);
        DataContractRegistry.register(PeerActiveEvent.class);
        DataContractRegistry.register(PeerInactiveEvent.class);
    }
}