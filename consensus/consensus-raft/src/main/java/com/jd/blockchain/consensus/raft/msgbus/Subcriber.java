package com.jd.blockchain.consensus.raft.msgbus;

public interface Subcriber {

    void onMessage(byte[] message);

    void onQuit();
}
