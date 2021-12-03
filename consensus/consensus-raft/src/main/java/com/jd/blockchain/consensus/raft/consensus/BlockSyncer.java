package com.jd.blockchain.consensus.raft.consensus;

import com.jd.blockchain.crypto.HashDigest;

public interface BlockSyncer {

    ManagerInfo getConsensusNodeManagerInfo(String consensusHost, int consensusPort);

    boolean sync(String consensusHost, int consensusPort, HashDigest ledger, long height) throws BlockSyncException;


    class ManagerInfo {
        private int managerPort;
        private boolean sslEnabled;

        public ManagerInfo(int managerPort, boolean sslEnabled) {
            this.managerPort = managerPort;
            this.sslEnabled = sslEnabled;
        }

        public int getManagerPort() {
            return managerPort;
        }

        public void setManagerPort(int managerPort) {
            this.managerPort = managerPort;
        }

        public boolean isSslEnabled() {
            return sslEnabled;
        }

        public void setSslEnabled(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
        }
    }
}
