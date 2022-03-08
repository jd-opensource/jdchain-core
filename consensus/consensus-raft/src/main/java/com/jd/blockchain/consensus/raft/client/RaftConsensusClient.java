package com.jd.blockchain.consensus.raft.client;

import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.consensus.manage.ConsensusManageClient;
import com.jd.blockchain.consensus.manage.ConsensusManageService;
import com.jd.blockchain.consensus.raft.settings.RaftClientSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftConsensusClient implements ConsensusClient, ConsensusManageClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConsensusClient.class);

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
        LOGGER.info("connect ledger: {} with settings: {}, raftMessageService: {}", ledgerHash, settings.getCurrentPeers(), raftMessageService);
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

    public synchronized boolean isInit() {
        return this.raftMessageService != null;
    }

    @Override
    public ConsensusManageService getManageService() {
        return this.raftMessageService;
    }
}
