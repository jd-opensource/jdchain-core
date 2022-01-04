package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.raft.settings.RaftNetworkSettings;

public class RaftNetworkConfig extends PropertyConfig implements RaftNetworkSettings {

    @ConfigProperty("system.server.rpc.connect.timeout")
    private int rpcConnectTimeoutMs = 10 * 1000;

    @ConfigProperty("system.server.rpc.default.timeout")
    private int rpcDefaultTimeoutMs = 10 * 1000;

    @ConfigProperty("system.server.rpc.snapshot.timeout")
    private int rpcSnapshotTimeoutMs = 5 * 60 * 1000;

    @ConfigProperty("system.server.rpc.request.timeout")
    private int rpcRequestTimeoutMs = 20 * 1000;

    public RaftNetworkConfig() {
    }

    public RaftNetworkConfig(int rpcConnectTimeoutMs, int rpcDefaultTimeoutMs, int rpcSnapshotTimeoutMs, int rpcRequestTimeoutMs) {
        this.rpcConnectTimeoutMs = rpcConnectTimeoutMs;
        this.rpcDefaultTimeoutMs = rpcDefaultTimeoutMs;
        this.rpcSnapshotTimeoutMs = rpcSnapshotTimeoutMs;
        this.rpcRequestTimeoutMs = rpcRequestTimeoutMs;
    }

    public RaftNetworkConfig(RaftNetworkSettings raftNetworkSettings) {
        this(raftNetworkSettings.getRpcConnectTimeoutMs(),
                raftNetworkSettings.getRpcDefaultTimeoutMs(),
                raftNetworkSettings.getRpcSnapshotTimeoutMs(),
                raftNetworkSettings.getRpcRequestTimeoutMs());
    }

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
