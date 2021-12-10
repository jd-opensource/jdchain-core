package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;

public class QueryLeaderRequest implements Serializable {
    private static final long serialVersionUID = -6124381895768053613L;

    @Override
    public String toString() {
        return "QueryLeaderRequest{}";
    }
}
