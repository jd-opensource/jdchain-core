package com.jd.blockchain.consensus.raft.consensus;

public class BlockSyncException extends Exception {


    public BlockSyncException() {
    }

    public BlockSyncException(String message) {
        super(message);
    }

    public BlockSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockSyncException(Throwable cause) {
        super(cause);
    }

}
