package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftServerSettings;

public class RaftServerSettingsConfig implements RaftServerSettings {

    private String realmName;
    private NodeSettings replicaSettings;
    private RaftConsensusSettings consensusSettings;

    @Override
    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public NodeSettings getReplicaSettings() {
        return replicaSettings;
    }

    public void setReplicaSettings(NodeSettings replicaSettings) {
        this.replicaSettings = replicaSettings;
    }

    @Override
    public RaftConsensusSettings getConsensusSettings() {
        return consensusSettings;
    }

    public void setConsensusSettings(RaftConsensusSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
    }
}
