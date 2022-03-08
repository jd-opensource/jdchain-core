package com.jd.blockchain.consensus.raft.manager;

import com.jd.blockchain.consensus.manage.ConsensusView;
import utils.net.NetworkAddress;

import java.util.concurrent.atomic.AtomicInteger;

public class RaftConsensusView implements ConsensusView {

    private static final AtomicInteger VIEW_ID_SEQ = new AtomicInteger(0);

    private int viewID;

    private RaftNodeInfo[] oldPeers;

    private RaftNodeInfo[] newPeers;

    public RaftConsensusView() {
        this.viewID = VIEW_ID_SEQ.incrementAndGet();
    }

    @Override
    public int getViewID() {
        return this.viewID;
    }

    public void setViewID(int viewID) {
        this.viewID = viewID;
    }

    @Override
    public Node[] getNodes() {
        return this.newPeers;
    }

    public RaftNodeInfo[] getOldPeers() {
        return oldPeers;
    }

    public void setOldPeers(RaftNodeInfo[] oldPeers) {
        this.oldPeers = oldPeers;
    }

    public RaftNodeInfo[] getNewPeers() {
        return newPeers;
    }

    public void setNewPeers(RaftNodeInfo[] newPeers) {
        this.newPeers = newPeers;
    }

    public static class RaftNodeInfo implements Node {

        private int replicaId;

        private NetworkAddress networkAddress;

        public RaftNodeInfo() {
        }

        public RaftNodeInfo(int replicaId, NetworkAddress networkAddress) {
            this.replicaId = replicaId;
            this.networkAddress = networkAddress;
        }

        @Override
        public int getReplicaId() {
            return replicaId;
        }

        public void setReplicaId(int replicaId) {
            this.replicaId = replicaId;
        }

        @Override
        public NetworkAddress getNetworkAddress() {
            return networkAddress;
        }

        public void setNetworkAddress(NetworkAddress networkAddress) {
            this.networkAddress = networkAddress;
        }
    }

}
