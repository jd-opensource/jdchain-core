package com.jd.blockchain.consensus.raft.consensus;

import java.util.ArrayDeque;
import java.util.List;

public class RaftBlockProposer implements BlockProposer{

    private ArrayDeque<Block> proposalBlockCache = new ArrayDeque<>();


    @Override
    public Block proposeBlock(List<byte[]> txs) {
        return null;
    }

    @Override
    public boolean canPropose() {
        return false;
    }
}
