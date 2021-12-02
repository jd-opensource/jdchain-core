package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;

public class ParticipantNodeRemoveRequest implements Serializable {

    private static final long serialVersionUID = 3278925499568973478L;

    private String host;

    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
