package com.jd.blockchain.consensus.raft.settings;

import com.jd.blockchain.consensus.service.ServerSettings;

public interface RaftServerSettings extends ServerSettings {

    RaftConsensusSettings getConsensusSettings();

    default RaftNetworkSettings getRaftNetworkSettings() {
        return getConsensusSettings().getNetworkSettings();
    }

    default RaftSettings getRaftSettings() {
        return getConsensusSettings().getRaftSettings();
    }

    default int getMaxTxsPerBlock() {
        return getConsensusSettings().getMaxTxsPerBlock();
    }

    default int getMaxBlockBytes() {
        return getConsensusSettings().getMaxBlockBytes();
    }

    default int getElectionTimeoutMs() {
        return getConsensusSettings().getElectionTimeoutMs();
    }

    default int getSnapshotIntervalSec() {
        return getConsensusSettings().getSnapshotIntervalSec();
    }

    default RaftNodeSettings getRaftNodeSettings() {
        return (RaftNodeSettings) getReplicaSettings();
    }
}

