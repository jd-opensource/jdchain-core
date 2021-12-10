package com.jd.blockchain.consensus.raft.client;

import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.consensus.manage.ConsensusManageClient;
import com.jd.blockchain.consensus.manage.ConsensusManageService;
import com.jd.blockchain.consensus.raft.settings.RaftClientSettings;

public class RaftConsensusClient implements ConsensusClient, ConsensusManageClient {

    private String ledgerHash;

    private RaftClientSettings settings;

    private RaftMessageService raftMessageService;

    public RaftConsensusClient(RaftClientSettings settings) {
        this.settings = settings;
    }

    public String getLedgerHash() {
        return ledgerHash;
    }

    public void setLedgerHash(String ledgerHash) {
        this.ledgerHash = ledgerHash;
    }

    @Override
    public MessageService getMessageService() {
        return this.raftMessageService;
    }


    @Override
    public ClientSettings getSettings() {
        return settings;
    }

    @Override
    public boolean isConnected() {
        if (this.raftMessageService != null) {
            return this.raftMessageService.isConnected();
        }
        return false;
    }

    @Override
    public synchronized void connect() {
        if (this.raftMessageService == null) {
            this.raftMessageService = new RaftMessageService(ledgerHash, settings);
            this.raftMessageService.init();
        }
    }

    @Override
    public void close() {
        if (this.raftMessageService != null) {
            this.raftMessageService.close();
        }
        this.raftMessageService = null;
    }

    @Override
    public ConsensusManageService getManageService() {
        return this.raftMessageService;
    }
}
