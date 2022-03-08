package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;
import java.util.Arrays;

public class RpcResponse implements Serializable {

    private static final long serialVersionUID = -5881270286502079421L;

    private byte[] result;

    private int errorCode;

    private String errorMessage;

    private boolean success;

    private boolean isRedirect;

    private String leaderEndpoint;

    public static RpcResponse success(byte[] result){
        RpcResponse txResponse = new RpcResponse();
        txResponse.setSuccess(true);
        txResponse.setRedirect(false);
        txResponse.setResult(result);
        return txResponse;
    }

    public static RpcResponse fail(int errorCode, String errorMessage){
        RpcResponse txResponse = new RpcResponse();
        txResponse.setSuccess(false);
        txResponse.setRedirect(false);
        txResponse.setErrorCode(errorCode);
        txResponse.setErrorMessage(errorMessage);
        return txResponse;
    }

    public static RpcResponse redirect(String leaderEndpoint){
        RpcResponse txResponse = new RpcResponse();
        txResponse.setSuccess(false);
        txResponse.setRedirect(true);
        txResponse.setErrorMessage("redirect request to leader");
        txResponse.setLeaderEndpoint(leaderEndpoint);
        return txResponse;
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public void setRedirect(boolean redirect) {
        isRedirect = redirect;
    }

    public String getLeaderEndpoint() {
        return leaderEndpoint;
    }

    public void setLeaderEndpoint(String leaderEndpoint) {
        this.leaderEndpoint = leaderEndpoint;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "result[size]=" + (result == null ? "null" : result.length) +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", success=" + success +
                ", isRedirect=" + isRedirect +
                ", leaderEndpoint='" + leaderEndpoint + '\'' +
                '}';
    }
}
