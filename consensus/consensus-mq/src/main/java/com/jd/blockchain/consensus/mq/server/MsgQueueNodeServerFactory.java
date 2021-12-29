package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.mq.config.MsgQueueNodeConfig;
import com.jd.blockchain.consensus.mq.config.MsgQueueServerConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServerFactory;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import utils.io.Storage;
import utils.net.SSLSecurity;

public class MsgQueueNodeServerFactory implements NodeServerFactory {

    @Override
    public MsgQueueServerSettings buildServerSettings(String realmName, ConsensusViewSettings consensusSetting, String nodeAddress) {
        return buildServerSettings(realmName, consensusSetting, nodeAddress, new SSLSecurity());
    }

    @Override
    public MsgQueueServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings, String nodeAddress, SSLSecurity sslSecurity) {
        if (!(viewSettings instanceof MsgQueueConsensusSettings)) {
            throw new IllegalArgumentException("ConsensusSettings data isn't supported! Accept MsgQueueConsensusSettings only!");
        }

        int id = -1;
        for (NodeSettings nodeSettings : viewSettings.getNodes()) {
            MsgQueueNodeSettings settings = (MsgQueueNodeSettings) nodeSettings;
            if (settings.getAddress().equals(nodeAddress)) {
                id = settings.getId();
                break;
            }
        }
        MsgQueueNodeSettings nodeSettings = new MsgQueueNodeConfig().setAddress(nodeAddress).setId(id);

        MsgQueueServerSettings serverSettings = new MsgQueueServerConfig().setRealmName(realmName)
                .setNodeSettings(nodeSettings).setConsensusSettings((MsgQueueConsensusSettings) viewSettings);
        return serverSettings;
    }

    @Override
    public MsgQueueNodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler,
                                          StateMachineReplicate stateMachineReplicator, Storage storage) {
        if (!(serverSettings instanceof MsgQueueServerSettings)) {
            throw new IllegalArgumentException("ServerSettings data isn't supported! Accept MsgQueueServerSettings only!");
        }

        return new MsgQueueNodeServer((MsgQueueServerSettings) serverSettings, messageHandler, stateMachineReplicator);
    }
}