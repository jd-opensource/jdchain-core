package com.jd.blockchain.consensus.raft.config;

import com.alipay.sofa.jraft.option.RaftOptions;
import com.jd.blockchain.consensus.raft.settings.RaftSettings;

public class RaftConfig extends PropertyConfig implements RaftSettings {

    @ConfigProperty("system.raft.maxByteCountPerRpc")
    private int maxByteCountPerRpc = 128 * 1024;

    @ConfigProperty("system.raft.maxEntriesSize")
    private int maxEntriesSize = 1024;

    @ConfigProperty("system.raft.maxBodySize")
    private int maxBodySize = 512 * 1024;

    @ConfigProperty("system.raft.maxAppendBufferSize")
    private int maxAppendBufferSize = 256 * 1024;

    @ConfigProperty("system.raft.maxElectionDelayMs")
    private int maxElectionDelayMs = 1000;

    @ConfigProperty("system.raft.electionHeartbeatFactor")
    private int electionHeartbeatFactor = 10;

    @ConfigProperty("system.raft.applyBatch")
    private int applyBatch = 32;

    @ConfigProperty(value = "system.raft.sync", type = ConfigPropertyType.BOOLEAN)
    private boolean sync = true;

    @ConfigProperty(value = "system.raft.syncMeta", type = ConfigPropertyType.BOOLEAN)
    private boolean syncMeta = false;

    @ConfigProperty("system.raft.disruptorBufferSize")
    private int disruptorBufferSize = 16384;

    @ConfigProperty(value = "system.raft.replicatorPipeline", type = ConfigPropertyType.BOOLEAN)
    private boolean replicatorPipeline = true;

    @ConfigProperty("system.raft.maxReplicatorInflightMsgs")
    private int maxReplicatorInflightMsgs = 256;

    public static RaftOptions buildRaftOptions(RaftSettings raftSettings) {

        RaftOptions raftOptions = new RaftOptions();

        raftOptions.setMaxByteCountPerRpc(raftSettings.getMaxByteCountPerRpc());
        raftOptions.setMaxEntriesSize(raftSettings.getMaxEntriesSize());
        raftOptions.setMaxBodySize(raftSettings.getMaxBodySize());
        raftOptions.setMaxAppendBufferSize(raftSettings.getMaxAppendBufferSize());
        raftOptions.setMaxElectionDelayMs(raftSettings.getMaxElectionDelayMs());
        raftOptions.setElectionHeartbeatFactor(raftSettings.getElectionHeartbeatFactor());
        raftOptions.setApplyBatch(raftSettings.getApplyBatch());
        raftOptions.setSync(raftSettings.isSync());
        raftOptions.setSyncMeta(raftSettings.isSyncMeta());
        raftOptions.setDisruptorBufferSize(raftSettings.getDisruptorBufferSize());
        raftOptions.setReplicatorPipeline(raftSettings.isReplicatorPipeline());
        raftOptions.setMaxReplicatorInflightMsgs(raftSettings.getMaxReplicatorInflightMsgs());

        return raftOptions;
    }

    public int getMaxByteCountPerRpc() {
        return maxByteCountPerRpc;
    }

    public void setMaxByteCountPerRpc(int maxByteCountPerRpc) {
        this.maxByteCountPerRpc = maxByteCountPerRpc;
    }

    public int getMaxEntriesSize() {
        return maxEntriesSize;
    }

    public void setMaxEntriesSize(int maxEntriesSize) {
        this.maxEntriesSize = maxEntriesSize;
    }

    public int getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public int getMaxAppendBufferSize() {
        return maxAppendBufferSize;
    }

    public void setMaxAppendBufferSize(int maxAppendBufferSize) {
        this.maxAppendBufferSize = maxAppendBufferSize;
    }

    public int getMaxElectionDelayMs() {
        return maxElectionDelayMs;
    }

    public void setMaxElectionDelayMs(int maxElectionDelayMs) {
        this.maxElectionDelayMs = maxElectionDelayMs;
    }

    public int getElectionHeartbeatFactor() {
        return electionHeartbeatFactor;
    }

    public void setElectionHeartbeatFactor(int electionHeartbeatFactor) {
        this.electionHeartbeatFactor = electionHeartbeatFactor;
    }

    public int getApplyBatch() {
        return applyBatch;
    }

    public void setApplyBatch(int applyBatch) {
        this.applyBatch = applyBatch;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public boolean isSyncMeta() {
        return syncMeta;
    }

    public void setSyncMeta(boolean syncMeta) {
        this.syncMeta = syncMeta;
    }

    public int getDisruptorBufferSize() {
        return disruptorBufferSize;
    }

    public void setDisruptorBufferSize(int disruptorBufferSize) {
        this.disruptorBufferSize = disruptorBufferSize;
    }

    public boolean isReplicatorPipeline() {
        return replicatorPipeline;
    }

    public void setReplicatorPipeline(boolean replicatorPipeline) {
        this.replicatorPipeline = replicatorPipeline;
    }

    public int getMaxReplicatorInflightMsgs() {
        return maxReplicatorInflightMsgs;
    }

    public void setMaxReplicatorInflightMsgs(int maxReplicatorInflightMsgs) {
        this.maxReplicatorInflightMsgs = maxReplicatorInflightMsgs;
    }
}
