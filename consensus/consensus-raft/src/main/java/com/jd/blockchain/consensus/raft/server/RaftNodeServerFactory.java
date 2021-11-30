package com.jd.blockchain.consensus.raft.server;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.config.RaftServerSettingsConfig;
import com.jd.blockchain.consensus.raft.server.RaftNodeServer;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftServerSettings;
import com.jd.blockchain.consensus.service.*;
import utils.io.Storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RaftNodeServerFactory implements NodeServerFactory {

    private static Map<String, NodeSettings[]> realmNodesMap = new ConcurrentHashMap<>();

    @Override
    public ServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings, String nodeAddress) {

        if (!(viewSettings instanceof RaftConsensusSettings)) {
            throw new IllegalStateException("view settings should be raft-consensus settings");
        }

        RaftServerSettingsConfig settingsConfig = new RaftServerSettingsConfig();

        NodeSettings currentNodeSettings = null;
        for (NodeSettings nodeSettings : viewSettings.getNodes()) {
            if (nodeSettings.getAddress().equals(nodeAddress)) {
                currentNodeSettings = nodeSettings;
                break;
            }
        }

        if (currentNodeSettings == null) {
            throw new IllegalArgumentException("node address does not exist in view settings!");
        }

        settingsConfig.setRealmName(realmName);
        settingsConfig.setConsensusSettings((RaftConsensusSettings) viewSettings);
        settingsConfig.setReplicaSettings(currentNodeSettings);

        return settingsConfig;
    }


    @Override
    public NodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler, StateMachineReplicate stateMachineReplicator, Storage runtimeStorage) {

        if(!(serverSettings instanceof RaftServerSettings)){
            throw new IllegalStateException("server settings should be raft-server settings");
        }

        return new RaftNodeServer(serverSettings.getRealmName(), (RaftServerSettings) serverSettings, messageHandler);
    }
}
