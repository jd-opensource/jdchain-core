package com.jd.blockchain.consensus.raft;

import com.jd.blockchain.consensus.ConsensusSettingsBuilder;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.raft.config.*;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import org.springframework.core.io.ClassPathResource;
import utils.PropertiesUtils;
import utils.io.BytesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class RaftConsensusSettingsBuilder implements ConsensusSettingsBuilder {

    private static final String CONFIG_FILE = "raft.config";

    private static Properties CONFIG_TEMPLATE;

    static {
        ClassPathResource configResource = new ClassPathResource(CONFIG_FILE);
        try {
            try (InputStream in = configResource.getInputStream()) {
                CONFIG_TEMPLATE = PropertiesUtils.load(in, BytesUtils.DEFAULT_CHARSET);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    @Override
    public ConsensusViewSettings createSettings(Properties props, Replica[] replicas) {

        RaftConsensusConfig raftConsensusConfig = new RaftConsensusConfig();

        List<NodeSettings> nodeSettings = new ArrayList<>(replicas.length);
        RaftConfig raftSettings = new RaftConfig();
        RaftNetworkConfig networkConfig = new RaftNetworkConfig();

        for (Replica replica : replicas) {
            RaftNodeConfig raftNodeSettings = new RaftNodeConfig();
            raftNodeSettings.init(props, replica);
            nodeSettings.add(raftNodeSettings);
        }

        raftSettings.init(props);
        networkConfig.init(props);
        raftConsensusConfig.init(props);

        raftConsensusConfig.setNodeSettingsList(nodeSettings);
        raftConsensusConfig.setRaftSettings(raftSettings);
        raftConsensusConfig.setNetworkSettings(networkConfig);

        return raftConsensusConfig;
    }

    @Override
    public Properties createPropertiesTemplate() {
        return PropertiesUtils.cloneFrom(CONFIG_TEMPLATE);
    }

    @Override
    public Properties convertToProperties(ConsensusViewSettings settings) {

        if (!(settings instanceof RaftConsensusSettings)) {
            throw new IllegalStateException("settings should be raft-consensus-settings type");
        }

        Properties properties = new Properties();

        RaftConsensusConfig raftConsensusSettings = (RaftConsensusConfig) settings;
        PropertiesUtils.mergeFrom(properties, raftConsensusSettings.convert());

        for (NodeSettings nodeSettings : raftConsensusSettings.getNodes()) {
            PropertiesUtils.mergeFrom(properties, ((RaftNodeConfig) nodeSettings).convert());
        }

        RaftConfig raftConfig = (RaftConfig) raftConsensusSettings.getRaftSettings();
        PropertiesUtils.mergeFrom(properties, raftConfig.convert());

        RaftNetworkConfig raftNetworkConfig = (RaftNetworkConfig) raftConsensusSettings.getNetworkSettings();
        PropertiesUtils.mergeFrom(properties, raftNetworkConfig.convert());

        return properties;
    }

    @Override
    public ConsensusViewSettings addReplicaSetting(ConsensusViewSettings viewSettings, Replica replica) {

        if (!(viewSettings instanceof RaftConsensusSettings)) {
            throw new IllegalArgumentException("The specified view-settings is not a raft-consensus-settings!");
        }
        if (!(replica instanceof RaftReplica)) {
            throw new IllegalArgumentException("The specified replica is not a raft-replica!");
        }
        RaftConsensusConfig raftConsensusSettings = (RaftConsensusConfig) viewSettings;
        RaftReplica newReplica = (RaftReplica) replica;

        NodeSettings[] origNodes = raftConsensusSettings.getNodes();
        if (origNodes.length == 0) {
            throw new IllegalStateException("The number of nodes is zero!");
        }

        RaftNodeSettings[] newNodes = new RaftNodeSettings[origNodes.length + 1];
        for (int i = 0; i < origNodes.length; i++) {
            newNodes[i] = (RaftNodeSettings) origNodes[i];
        }

        RaftNodeConfig newRaftNodeConfig = new RaftNodeConfig();
        newRaftNodeConfig.setId(newReplica.getId());
        newRaftNodeConfig.setAddress(newReplica.getAddress().toBase58());
        newRaftNodeConfig.setPubKey(newReplica.getPubKey());
        newRaftNodeConfig.setNetworkAddress(newReplica.getNetworkAddress());
        newRaftNodeConfig.setRaftPath(newReplica.getRaftPath());
        newNodes[origNodes.length] = newRaftNodeConfig;

        RaftConsensusConfig raftConsensusConfig = new RaftConsensusConfig(raftConsensusSettings);
        raftConsensusConfig.setNodeSettingsList(Arrays.asList(newNodes));

        return raftConsensusConfig;
    }

    @Override
    public ConsensusViewSettings updateSettings(ConsensusViewSettings oldConsensusSettings, Properties newProps) {
        //todo
        return null;
    }
}
