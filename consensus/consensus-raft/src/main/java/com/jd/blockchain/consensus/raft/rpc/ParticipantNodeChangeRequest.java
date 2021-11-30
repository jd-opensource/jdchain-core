package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;

public class ParticipantNodeChangeRequest implements Serializable {

    private static final long serialVersionUID = -4730986208462140770L;

    private String nodeHost;

    private int nodePort;

    private ChangeType changeType;

    public String getNodeHost() {
        return nodeHost;
    }

    public void setNodeHost(String nodeHost) {
        this.nodeHost = nodeHost;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public enum ChangeType {

        ADD,

        REMOVE

    }
}
