package com.jd.blockchain.consensus.raft.consensus;

import com.alipay.sofa.jraft.util.Endpoint;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.httpservice.agent.ServiceEndpoint;

public interface BlockSyncer {

    void sync(ServiceEndpoint serviceEndpoint, HashDigest ledger, long height) throws BlockSyncException;

    void sync(Endpoint peerEndpoint, HashDigest ledger, long height) throws BlockSyncException;

    boolean isSyncing();

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
