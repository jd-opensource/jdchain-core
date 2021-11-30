package com.jd.blockchain.consensus.raft.server;

import com.jd.blockchain.consensus.service.ConsensusMessageContext;

public class RaftConsensusMessageContext  implements ConsensusMessageContext {

    private String realmName;

    private String batchId;

    private long timestamp;

    public static RaftConsensusMessageContext createContext(String realmName){
        RaftConsensusMessageContext context = new RaftConsensusMessageContext();
        context.setRealmName(realmName);
        return context;
    }


    @Override
    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
