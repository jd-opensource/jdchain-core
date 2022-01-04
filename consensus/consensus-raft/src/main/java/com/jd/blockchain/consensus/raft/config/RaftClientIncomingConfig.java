package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.raft.RaftConsensusProvider;
import com.jd.blockchain.consensus.raft.settings.RaftClientIncomingSettings;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.crypto.PubKey;

public class RaftClientIncomingConfig implements RaftClientIncomingSettings {

    private int clientId;

    private PubKey pubKey;

    private String[] currentPeers;

    private RaftConsensusSettings consensusSettings;

    private SessionCredential sessionCredential;

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }

    public RaftConsensusSettings getConsensusSettings() {
        return consensusSettings;
    }

    public void setConsensusSettings(RaftConsensusSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
    }

    public SessionCredential getSessionCredential() {
        return sessionCredential;
    }

    public void setSessionCredential(SessionCredential sessionCredential) {
        this.sessionCredential = sessionCredential;
    }

    @Override
    public PubKey getPubKey() {
        return pubKey;
    }

    @Override
    public String[] getCurrentPeers() {
        return currentPeers;
    }

    public void setCurrentPeers(String[] currentPeers) {
        this.currentPeers = currentPeers;
    }

    @Override
    public int getClientId() {
        return clientId;
    }


    @Override
    public String getProviderName() {
        return RaftConsensusProvider.PROVIDER_NAME;
    }


    @Override
    public ConsensusViewSettings getViewSettings() {
        return consensusSettings;
    }


    @Override
    public SessionCredential getCredential() {
        return sessionCredential;
    }
}