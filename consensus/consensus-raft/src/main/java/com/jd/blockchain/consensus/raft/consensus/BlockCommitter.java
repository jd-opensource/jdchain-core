package com.jd.blockchain.consensus.raft.consensus;

import com.jd.blockchain.consensus.raft.server.BlockClosure;

public interface BlockCommitter {

    boolean commitBlock(Block block, BlockClosure done) throws BlockCommittedException;

    void registerCallBack(BlockCommitCallback callback);

}
