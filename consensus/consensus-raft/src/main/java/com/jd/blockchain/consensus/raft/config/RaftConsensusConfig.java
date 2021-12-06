package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNetworkSettings;
import com.jd.blockchain.consensus.raft.settings.RaftSettings;
import utils.Property;

import java.util.ArrayList;
import java.util.List;

public class RaftConsensusConfig extends PropertyConfig implements RaftConsensusSettings {

    @ConfigProperty("system.server.block.max.num")
    private int maxTxsPerBlock;

    @ConfigProperty("system.server.block.max.bytes")
    private int maxBlockBytes;

    @ConfigProperty("system.server.election.timeout")
    private int electionTimeoutMs;

    @ConfigProperty("system.server.snapshot.interval")
    private int snapshotIntervalSec;

    private List<NodeSettings> nodeSettingsList = new ArrayList<>();

    private RaftNetworkSettings networkSettings;

    private RaftSettings raftSettings;

    public RaftConsensusConfig() {
    }

    public RaftConsensusConfig(RaftConsensusConfig oldRaftConsensusConfig) {
        super();
        this.maxBlockBytes = oldRaftConsensusConfig.getMaxBlockBytes();
        this.maxBlockBytes = oldRaftConsensusConfig.getMaxBlockBytes();
        this.electionTimeoutMs = oldRaftConsensusConfig.getElectionTimeoutMs();
        this.snapshotIntervalSec = oldRaftConsensusConfig.getSnapshotIntervalSec();
        this.nodeSettingsList = oldRaftConsensusConfig.getNodeSettingsList();
        this.networkSettings = oldRaftConsensusConfig.getNetworkSettings();
        this.raftSettings = oldRaftConsensusConfig.getRaftSettings();
    }


    //适配前端获取共识属性方法
    public Property[] getSystemConfigs() {

        List<Property> propertyList = new ArrayList<>();

        this.convert().forEach((k, v) -> propertyList.add(new Property((String) k, (String) v)));
        if (this.networkSettings != null) {
            ((RaftNetworkConfig) this.networkSettings).convert().forEach((k, v) -> propertyList.add(new Property((String) k, (String) v)));
        }

        if (this.raftSettings != null) {
            ((RaftConfig) this.raftSettings).convert().forEach((k, v) -> propertyList.add(new Property((String) k, (String) v)));
        }

        return propertyList.toArray(new Property[propertyList.size()]);
    }

    @Override
    public int getMaxTxsPerBlock() {
        return maxTxsPerBlock;
    }

    public void setMaxTxsPerBlock(int maxTxsPerBlock) {
        this.maxTxsPerBlock = maxTxsPerBlock;
    }

    @Override
    public int getMaxBlockBytes() {
        return maxBlockBytes;
    }

    public void setMaxBlockBytes(int maxBlockBytes) {
        this.maxBlockBytes = maxBlockBytes;
    }

    @Override
    public int getElectionTimeoutMs() {
        return electionTimeoutMs;
    }

    public void setElectionTimeoutMs(int electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
    }

    @Override
    public int getSnapshotIntervalSec() {
        return snapshotIntervalSec;
    }

    public void setSnapshotIntervalSec(int snapshotIntervalSec) {
        this.snapshotIntervalSec = snapshotIntervalSec;
    }

    @Override
    public NodeSettings[] getNodes() {
        return nodeSettingsList.toArray(new NodeSettings[nodeSettingsList.size()]);
    }

    public List<NodeSettings> getNodeSettingsList() {
        return nodeSettingsList;
    }

    public void setNodeSettingsList(List<NodeSettings> nodeSettingsList) {
        this.nodeSettingsList = nodeSettingsList;
    }

    @Override
    public RaftSettings getRaftSettings() {
        return raftSettings;
    }

    public void setRaftSettings(RaftSettings raftSettings) {
        this.raftSettings = raftSettings;
    }

    @Override
    public RaftNetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    public void setNetworkSettings(RaftNetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
    }

}