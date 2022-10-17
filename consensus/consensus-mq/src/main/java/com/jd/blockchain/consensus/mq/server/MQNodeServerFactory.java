package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.mq.config.MQNodeConfig;
import com.jd.blockchain.consensus.mq.config.MQServerConfig;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MQNodeSettings;
import com.jd.blockchain.consensus.mq.settings.MQServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServerFactory;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import utils.io.Storage;
import utils.net.SSLSecurity;

import java.util.Properties;

public class MQNodeServerFactory implements NodeServerFactory {

    @Override
    public MQServerSettings buildServerSettings(
            String realmName, ConsensusViewSettings consensusSetting, String nodeAddress) {
        return buildServerSettings(realmName, consensusSetting, nodeAddress, new SSLSecurity(), null);
    }

    @Override
    public MQServerSettings buildServerSettings(
            String realmName,
            ConsensusViewSettings viewSettings,
            String nodeAddress,
            SSLSecurity sslSecurity,
            Properties properties) {
        if (!(viewSettings instanceof MQConsensusSettings)) {
            throw new IllegalArgumentException(
                    "ConsensusSettings data isn't supported! Accept MsgQueueConsensusSettings only!");
        }

        int id = -1;
        String host = null;
        for (NodeSettings nodeSettings : viewSettings.getNodes()) {
            MQNodeSettings settings = (MQNodeSettings) nodeSettings;
            if (settings.getAddress().equals(nodeAddress)) {
                id = settings.getId();
                host = settings.getHost();
                break;
            }
        }
        MQNodeSettings nodeSettings =
                new MQNodeConfig().setAddress(nodeAddress).setId(id).setHost(host);

        MQServerSettings serverSettings =
                new MQServerConfig()
                        .setRealmName(realmName)
                        .setNodeSettings(nodeSettings)
                        .setConsensusSettings((MQConsensusSettings) viewSettings);
        return serverSettings;
    }

    @Override
    public MQNodeServer setupServer(
            ServerSettings serverSettings,
            MessageHandle messageHandler,
            StateMachineReplicate stateMachineReplicator,
            Storage storage) {
        if (!(serverSettings instanceof MQServerSettings)) {
            throw new IllegalArgumentException(
                    "ServerSettings data isn't supported! Accept MsgQueueServerSettings only!");
        }

        return new MQNodeServer(
                (MQServerSettings) serverSettings, messageHandler, stateMachineReplicator);
    }
}
