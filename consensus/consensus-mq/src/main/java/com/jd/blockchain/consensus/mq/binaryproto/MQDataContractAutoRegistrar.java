package com.jd.blockchain.consensus.mq.binaryproto;

import com.jd.binaryproto.DataContractAutoRegistrar;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.configure.ConsensusDataContractAutoRegistrar;
import com.jd.blockchain.consensus.mq.client.MQCredentialInfo;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings;

public class MQDataContractAutoRegistrar implements DataContractAutoRegistrar {

    @Override
    public int order() {
        return ConsensusDataContractAutoRegistrar.ORDER + 1;
    }

    @Override
    public void initContext(DataContractRegistry registry) {
        DataContractRegistry.register(MsgQueueConsensusSettings.class);
        DataContractRegistry.register(MsgQueueNodeSettings.class);
        DataContractRegistry.register(MsgQueueClientIncomingSettings.class);
        DataContractRegistry.register(MQCredentialInfo.class);
    }

}
