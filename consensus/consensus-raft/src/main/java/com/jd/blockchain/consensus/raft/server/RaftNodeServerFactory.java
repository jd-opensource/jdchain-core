package com.jd.blockchain.consensus.raft.server;

import com.google.common.base.Strings;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.config.RaftServerSettingsConfig;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftServerSettings;
import com.jd.blockchain.consensus.service.*;
import utils.io.Storage;
import utils.net.SSLSecurity;

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
    public ServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings, String nodeAddress, SSLSecurity sslSecurity) {
        if (sslSecurity != null && !Strings.isNullOrEmpty(sslSecurity.getKeyStore())) {
            System.getProperties().setProperty("bolt.server.ssl.enable", "true");
            System.getProperties().setProperty("bolt.server.ssl.clientAuth", "false");
            System.getProperties().setProperty("bolt.server.ssl.keystore", sslSecurity.getKeyStore());
            System.getProperties().setProperty("bolt.server.ssl.keystore.password", sslSecurity.getKeyStorePassword());
            System.getProperties().setProperty("bolt.server.ssl.keystore.type", sslSecurity.getKeyStoreType());
        }

        //two way
        if(sslSecurity != null && !Strings.isNullOrEmpty(sslSecurity.getTrustStore())){
            System.getProperties().setProperty("bolt.server.ssl.clientAuth", "true");
            System.getProperties().setProperty("bolt.client.ssl.enable", "true");
            System.getProperties().setProperty("bolt.client.ssl.keystore", sslSecurity.getTrustStore());
            System.getProperties().setProperty("bolt.client.ssl.keystore.password", sslSecurity.getTrustStorePassword());
            System.getProperties().setProperty("bolt.client.ssl.keystore.type", sslSecurity.getTrustStoreType());
        }

        return buildServerSettings(realmName, viewSettings, nodeAddress);
    }


    @Override
    public NodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler, StateMachineReplicate stateMachineReplicator, Storage runtimeStorage) {

        if (!(serverSettings instanceof RaftServerSettings)) {
            throw new IllegalStateException("server settings should be raft-server settings");
        }

        return new RaftNodeServer(serverSettings.getRealmName(), (RaftServerSettings) serverSettings, messageHandler);
    }
}
