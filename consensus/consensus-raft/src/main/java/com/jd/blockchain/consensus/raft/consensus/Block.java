package com.jd.blockchain.consensus.raft.consensus;

import java.io.Serializable;
import java.util.List;

public class Block implements Serializable {

    private static final long serialVersionUID = 4648845890303631523L;

    private long timestamp;

    private long height;

    private List<byte[]> txs;

    private String preBlockHash;

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
