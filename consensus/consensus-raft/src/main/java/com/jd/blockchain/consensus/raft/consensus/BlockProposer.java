package com.jd.blockchain.consensus.raft.consensus;

import java.util.List;

public interface BlockProposer {

    Block proposeBlock(List<byte[]> txs);

    boolean canPropose();

}
