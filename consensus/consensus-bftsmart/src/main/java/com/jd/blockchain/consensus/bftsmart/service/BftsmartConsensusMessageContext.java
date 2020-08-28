package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.service.ConsensusMessageContext;

public class BftsmartConsensusMessageContext implements ConsensusMessageContext {

    private String realmName;

    private String batchId;

    private long timestamp;

    public BftsmartConsensusMessageContext(String realmName) {
        this.realmName = realmName;
    }

    private BftsmartConsensusMessageContext(String realmName, long timestamp) {
        this.realmName = realmName;
        this.timestamp = timestamp;
    }

    @Override
    public String getBatchId() {
        return this.batchId;
    }

    @Override
    public String getRealmName() {
        return this.realmName;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static BftsmartConsensusMessageContext createInstance(String realmName, long timestamp) {
        return new BftsmartConsensusMessageContext(realmName, timestamp);
    }
}
