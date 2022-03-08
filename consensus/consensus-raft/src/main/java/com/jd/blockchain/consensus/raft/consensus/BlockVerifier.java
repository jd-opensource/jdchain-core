package com.jd.blockchain.consensus.raft.consensus;

public interface BlockVerifier {

    void verify(Block block);

}
