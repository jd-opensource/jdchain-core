package com.jd.blockchain.consensus.raft.consensus;

public class BlockCommittedException extends Exception {

    private static final long serialVersionUID = 8955068550710615185L;

    private long height;

    public BlockCommittedException() {
    }

    public BlockCommittedException(long height) {
        this.height = height;
    }

    public long getHeight() {
        return height;
    }

}
