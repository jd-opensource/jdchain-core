package com.jd.blockchain.consensus.raft.consensus;

public interface BlockCommitCallback {

    void commitCallBack(Block block, boolean isCommit);

}
