package com.jd.blockchain.consensus.raft.consensus;

public class BlockCommittedException extends Exception {

    private static final long serialVersionUID = 8955068550710615185L;

    private static final String MSG_FORMAT = "block height: %d has been committed";

    public BlockCommittedException() {
    }

    public BlockCommittedException(long height) {
        super(String.format(MSG_FORMAT, height));
    }


}
