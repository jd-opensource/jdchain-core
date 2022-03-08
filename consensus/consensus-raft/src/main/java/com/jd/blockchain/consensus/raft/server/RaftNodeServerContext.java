package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.entity.PeerId;
import com.jd.blockchain.crypto.HashDigest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RaftNodeServerContext {

    private static final RaftNodeServerContext INSTANCE = new RaftNodeServerContext();

    private Map<HashDigest, RaftNodeServer> NODE_SERVER_MAP = new ConcurrentHashMap<>();

    private RaftNodeServerContext() {
    }

    public static RaftNodeServerContext getInstance() {
        return INSTANCE;
    }

    public void init(RaftNodeServer nodeServer) {
        NODE_SERVER_MAP.put(nodeServer.getLedgerHashDigest(), nodeServer);
    }

    public PeerId getLeader(HashDigest ledger) {
        ensureInit(ledger);
        return NODE_SERVER_MAP.get(ledger).getLeader();
    }

    public Node getNode(HashDigest ledger) {
        ensureInit(ledger);
        return NODE_SERVER_MAP.get(ledger).getNode();
    }

    public boolean isLeader(HashDigest ledger) {
        ensureInit(ledger);
        return NODE_SERVER_MAP.get(ledger).isLeader();
    }

    public void refreshRouteTable(HashDigest ledger) {
        ensureInit(ledger);
        NODE_SERVER_MAP.get(ledger).refreshRouteTable();
    }

    private void ensureInit(HashDigest ledger) {
        if (!NODE_SERVER_MAP.containsKey(ledger)) {
            throw new IllegalStateException("ledger is not init");
        }
    }


}
