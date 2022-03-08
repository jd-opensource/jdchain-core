package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;

public class ParticipantNodeTransferRequest implements Serializable {

    private static final long serialVersionUID = -9157134572345136180L;

    private String preHost;

    private int prePort;

    private String newHost;

    private int newPort;

    public String getPreHost() {
        return preHost;
    }

    public void setPreHost(String preHost) {
        this.preHost = preHost;
    }

    public int getPrePort() {
        return prePort;
    }

    public void setPrePort(int prePort) {
        this.prePort = prePort;
    }

    public String getNewHost() {
        return newHost;
    }

    public void setNewHost(String newHost) {
        this.newHost = newHost;
    }

    public int getNewPort() {
        return newPort;
    }

    public void setNewPort(int newPort) {
        this.newPort = newPort;
    }

    @Override
    public String toString() {
        return "ParticipantNodeTransferRequest{" +
                "preHost='" + preHost + '\'' +
                ", prePort=" + prePort +
                ", newHost='" + newHost + '\'' +
                ", newPort=" + newPort +
                '}';
    }
}
