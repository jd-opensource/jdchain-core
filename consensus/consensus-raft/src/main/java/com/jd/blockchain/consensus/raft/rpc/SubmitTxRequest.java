package com.jd.blockchain.consensus.raft.rpc;

import java.io.Serializable;
import java.util.Base64;

public class SubmitTxRequest implements Serializable {

    private static final long serialVersionUID = 3119934252521509049L;

    private byte[] tx;

    public byte[] getTx() {
        return tx;
    }

    public void setTx(byte[] tx) {
        this.tx = tx;
    }

    public SubmitTxRequest() {
    }

    public SubmitTxRequest(byte[] tx) {
        this.tx = tx;
    }

    @Override
    public String toString() {
        return "SubmitTxRequest{" +
                "tx=" + Base64.getEncoder().encodeToString(tx) +
                '}';
    }
}
