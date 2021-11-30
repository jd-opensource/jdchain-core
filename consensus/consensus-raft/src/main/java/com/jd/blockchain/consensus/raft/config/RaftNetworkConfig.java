package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.raft.settings.RaftNetworkSettings;

import java.util.Properties;

public class RaftNetworkConfig extends PropertyConfig implements RaftNetworkSettings {

    @ConfigProperty("system.server.rpc.connect.timeout")
    private int rpcConnectTimeoutMs;

    @ConfigProperty("system.server.rpc.default.timeout")
    private int rpcDefaultTimeoutMs;

    @ConfigProperty("system.server.rpc.snapshot.timeout")
    private int rpcSnapshotTimeoutMs;

    @ConfigProperty("system.server.rpc.request.timeout")
    private int rpcRequestTimeoutMs;

    @Override
    public int getRpcConnectTimeoutMs() {
        return rpcConnectTimeoutMs;
    }

    public void setRpcConnectTimeoutMs(int rpcConnectTimeoutMs) {
        this.rpcConnectTimeoutMs = rpcConnectTimeoutMs;
    }

    @Override
    public int getRpcDefaultTimeoutMs() {
        return rpcDefaultTimeoutMs;
    }

    public void setRpcDefaultTimeoutMs(int rpcDefaultTimeoutMs) {
        this.rpcDefaultTimeoutMs = rpcDefaultTimeoutMs;
    }

    public int getRpcSnapshotTimeoutMs() {
        return rpcSnapshotTimeoutMs;
    }

    public void setRpcSnapshotTimeoutMs(int rpcSnapshotTimeoutMs) {
        this.rpcSnapshotTimeoutMs = rpcSnapshotTimeoutMs;
    }

    @Override
    public int getRpcRequestTimeoutMs() {
        return rpcRequestTimeoutMs;
    }

    public void setRpcRequestTimeoutMs(int rpcRequestTimeoutMs) {
        this.rpcRequestTimeoutMs = rpcRequestTimeoutMs;
    }


}
