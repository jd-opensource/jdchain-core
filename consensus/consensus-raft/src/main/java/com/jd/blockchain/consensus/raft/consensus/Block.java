package com.jd.blockchain.consensus.raft.consensus;

import java.io.Serializable;
import java.util.List;

public class Block implements Serializable {

    private static final long serialVersionUID = 4648845890303631523L;

    private long proposalTimestamp;

    private long height;

    private List<byte[]> txs;

    private String preBlockHash;

    //todo
    private String currentBlockHash;

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public List<byte[]> getTxs() {
        return txs;
    }

    public void setTxs(List<byte[]> txs) {
        this.txs = txs;
    }

    public String getPreBlockHash() {
        return preBlockHash;
    }

    public void setPreBlockHash(String preBlockHash) {
        this.preBlockHash = preBlockHash;
    }

    public String getCurrentBlockHash() {
        return currentBlockHash;
    }

    public void setCurrentBlockHash(String currentBlockHash) {
        this.currentBlockHash = currentBlockHash;
    }

    public long getProposalTimestamp() {
        return proposalTimestamp;
    }

    public void setProposalTimestamp(long proposalTimestamp) {
        this.proposalTimestamp = proposalTimestamp;
    }

    @Override
    public String toString() {
        return "Block{" +
                "proposalTimestamp=" + proposalTimestamp +
                ", height=" + height +
                ", txs[size]=" + txs.size() +
                ", preBlockHash='" + preBlockHash + '\'' +
                ", currentBlockHash='" + currentBlockHash + '\'' +
                '}';
    }
}
