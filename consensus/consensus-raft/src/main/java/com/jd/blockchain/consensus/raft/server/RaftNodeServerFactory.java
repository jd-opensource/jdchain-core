package com.jd.blockchain.consensus.raft.server;

import com.google.common.base.Strings;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.config.RaftNodeConfig;
import com.jd.blockchain.consensus.raft.config.RaftServerSettingsConfig;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.consensus.raft.settings.RaftServerSettings;
import com.jd.blockchain.consensus.service.*;
import utils.GmSSLProvider;
import utils.io.Storage;
import utils.net.SSLSecurity;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RaftNodeServerFactory implements NodeServerFactory {

    private static Map<String, NodeSettings[]> realmNodesMap = new ConcurrentHashMap<>();

    @Override
    public ServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings, String nodeAddress) {
        return buildServerSettings(realmName, viewSettings, nodeAddress, null);

    }


    @Override
    public ServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings, String nodeAddress, SSLSecurity sslSecurity) {

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

        if(sslSecurity == null){
            return  settingsConfig;
        }

        //TODO TLS适配
        boolean enableTLS = false;
        RaftNodeSettings raftNodeConfig = (RaftNodeSettings) currentNodeSettings;
        if(raftNodeConfig.getNetworkAddress().isSecure() && !Strings.isNullOrEmpty(sslSecurity.getKeyStore())){
            enableTLS = true;
            GmSSLProvider.enableGMSupport(sslSecurity.getProtocol());
        }

        if(!enableTLS){
            return  settingsConfig;
        }

        //Node节点作为服务端时， 配置私钥信息
        System.getProperties().setProperty("bolt.server.ssl.enable", "true");
        System.getProperties().setProperty("bolt.server.ssl.keystore", sslSecurity.getKeyStore());
        System.getProperties().setProperty("bolt.server.ssl.keystore.password", sslSecurity.getKeyStorePassword());
        System.getProperties().setProperty("bolt.server.ssl.keystore.type", sslSecurity.getKeyStoreType());
        System.getProperties().setProperty("bolt.ssl.protocol", nullToEmpty(sslSecurity.getProtocol()));
        if(sslSecurity.getEnabledProtocols() != null){
            System.getProperties().setProperty("bolt.ssl.enabled-protocols", String.join(",", sslSecurity.getEnabledProtocols()));
        }

        if(sslSecurity.getCiphers() != null){
            System.getProperties().setProperty("bolt.ssl.ciphers", String.join(",", sslSecurity.getCiphers()));
        }

        //Node节点配置信任证书，以及作为客户端链接其他节点时的信任证书
        if(!Strings.isNullOrEmpty(sslSecurity.getTrustStore())){
            //服务端配置: 此时服务端有keystore, truststore， 此时开启双向认证
            System.getProperties().setProperty("bolt.server.ssl.clientAuth", "true");
            System.getProperties().setProperty("bolt.client.ssl.enable", "true");
            System.getProperties().setProperty("bolt.client.ssl.keystore", sslSecurity.getTrustStore());
            System.getProperties().setProperty("bolt.client.ssl.keystore.password", sslSecurity.getTrustStorePassword());
            System.getProperties().setProperty("bolt.client.ssl.keystore.type", sslSecurity.getTrustStoreType());
        }

        return settingsConfig;
    }

    private String nullToEmpty(String str){
        if(str == null){
            return "";
        }

        return str;
    }


    @Override
    public NodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler, StateMachineReplicate stateMachineReplicator, Storage runtimeStorage) {

        if (!(serverSettings instanceof RaftServerSettings)) {
            throw new IllegalStateException("server settings should be raft-server settings");
        }

        return new RaftNodeServer(serverSettings.getRealmName(), (RaftServerSettings) serverSettings, messageHandler);
    }
}
