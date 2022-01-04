package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.raft.client.RaftSessionCredentialConfig;
import com.jd.blockchain.consensus.raft.settings.RaftClientIncomingSettings;
import com.jd.blockchain.consensus.raft.settings.RaftClientSettings;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.crypto.PubKey;

public class RaftClientConfig implements RaftClientSettings {

    private int clientId;

    private PubKey pubKey;

    private String[] currentPeers;

    private RaftConsensusSettings raftConsensusSettings;

    public RaftClientConfig(RaftClientIncomingSettings incomingSettings) {
        this.clientId = incomingSettings.getClientId();
        this.pubKey = incomingSettings.getPubKey();
        this.raftConsensusSettings = (RaftConsensusSettings) incomingSettings.getViewSettings();
        this.currentPeers = incomingSettings.getCurrentPeers();
    }

    @Override
    public String[] getCurrentPeers() {
        return currentPeers;
    }

    public void setCurrentPeers(String[] currentPeers) {
        this.currentPeers = currentPeers;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public PubKey getPubKey() {
        return pubKey;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }

    public RaftConsensusSettings getRaftConsensusSettings() {
        return raftConsensusSettings;
    }

    public void setRaftConsensusSettings(RaftConsensusSettings raftConsensusSettings) {
        this.raftConsensusSettings = raftConsensusSettings;
    }

    @Override
    public int getClientId() {
        return clientId;
    }


    @Override
    public PubKey getClientPubKey() {
        return pubKey;
    }


    @Override
    public ConsensusViewSettings getViewSettings() {
        return raftConsensusSettings;
    }

    @Override
    public SessionCredential getSessionCredential() {
        return RaftSessionCredentialConfig.createEmptyCredential();
    }
}