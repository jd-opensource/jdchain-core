package com.jd.blockchain.consensus.raft;

import com.google.common.base.Strings;
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
import java.util.*;

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

        RaftConsensusConfig raftConsensusConfig = new RaftConsensusConfig((RaftConsensusSettings) settings);

        PropertiesUtils.mergeFrom(properties, raftConsensusConfig.convert());
        for (NodeSettings nodeSettings : raftConsensusConfig.getNodes()) {
            PropertiesUtils.mergeFrom(properties, ((RaftNodeConfig) nodeSettings).convert());
        }

        RaftConfig raftConfig = (RaftConfig) raftConsensusConfig.getRaftSettings();
        PropertiesUtils.mergeFrom(properties, raftConfig.convert());

        RaftNetworkConfig raftNetworkConfig = (RaftNetworkConfig) raftConsensusConfig.getNetworkSettings();
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
        newNodes[origNodes.length] = newRaftNodeConfig;

        RaftConsensusConfig raftConsensusConfig = new RaftConsensusConfig(raftConsensusSettings);
        raftConsensusConfig.setNodeSettingsList(Arrays.asList(newNodes));

        return raftConsensusConfig;
    }

    @Override
    public ConsensusViewSettings updateSettings(ConsensusViewSettings oldConsensusSettings, Properties newProps) {

        if (newProps == null) {
            throw new IllegalArgumentException("updateSettings parameters error!");
        }

        RaftConsensusConfig raftConsensusConfig = new RaftConsensusConfig((RaftConsensusSettings) oldConsensusSettings);

        RaftConfig raftConfig = (RaftConfig) raftConsensusConfig.getRaftSettings();
        RaftNetworkConfig raftNetworkConfig = (RaftNetworkConfig) raftConsensusConfig.getNetworkSettings();

        RaftNodeConfig activeNode = getActiveOrUpdateNode(newProps);
        RaftNodeConfig deActiveNode = getDeActiveNode(newProps);

        Iterator<NodeSettings> iterator = raftConsensusConfig.getNodeSettingsList().iterator();
        while (iterator.hasNext()) {
            RaftNodeConfig nodeConfig = (RaftNodeConfig) iterator.next();
            if (deActiveNode != null && deActiveNode.getId() == nodeConfig.getId()) {
                iterator.remove();
            }
            //update node step
            if (activeNode != null && activeNode.getId() == nodeConfig.getId()) {
                iterator.remove();
            }
        }

        if (activeNode != null) {
            raftConsensusConfig.getNodeSettingsList().add(activeNode);
        }

        raftConsensusConfig.init(newProps);
        raftConfig.init(newProps);
        raftNetworkConfig.init(newProps);

        return raftConsensusConfig;
    }

    private RaftNodeConfig getActiveOrUpdateNode(Properties newProps) {
        String activeId = PropertiesUtils.getOptionalProperty(newProps, "active.participant.id");
        if (Strings.isNullOrEmpty(activeId)) {
            return null;
        }

        RaftNodeConfig raftNodeConfig = new RaftNodeConfig();
        raftNodeConfig.init(newProps, Integer.parseInt(activeId));
        return raftNodeConfig;
    }

    private RaftNodeConfig getDeActiveNode(Properties newProps) {
        String deActiveId = PropertiesUtils.getOptionalProperty(newProps, "deactive.participant.id");
        if (Strings.isNullOrEmpty(deActiveId)) {
            return null;
        }
        RaftNodeConfig raftNodeConfig = new RaftNodeConfig();
        raftNodeConfig.setId(Integer.parseInt(deActiveId));
        return raftNodeConfig;
    }


}
