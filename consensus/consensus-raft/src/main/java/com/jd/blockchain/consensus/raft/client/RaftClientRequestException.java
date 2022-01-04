package com.jd.blockchain.consensus.raft.client;

public class RaftClientRequestException extends RuntimeException {

    private static final long serialVersionUID = -807840587576395440L;

    public RaftClientRequestException() {
    }

    public RaftClientRequestException(String message) {
        super(message);
    }

    public RaftClientRequestException(String message, Throwable cause) {
        super(message, cause);
    }


    public RaftClientRequestException(Throwable cause) {
        super(cause);
    }

    public RaftClientRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
