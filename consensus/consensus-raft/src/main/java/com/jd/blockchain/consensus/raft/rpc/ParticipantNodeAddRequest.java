package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;

public class ParticipantNodeAddRequest implements Serializable {

    private static final long serialVersionUID = -5215413263877148164L;

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

    @Override
    public String toString() {
        return "ParticipantNodeAddRequest{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
