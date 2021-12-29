package com.jd.blockchain.consensus.mq.event;

import java.io.Serializable;

public class ExtendMessageResult implements Serializable {

    private boolean success;
    private String error;

    public ExtendMessageResult() {}

    public ExtendMessageResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public static ExtendMessageResult createSuccessResult() {
        return new ExtendMessageResult(true, null);
    }

    public static ExtendMessageResult createErrorResult(String error) {
        return new ExtendMessageResult(false, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
