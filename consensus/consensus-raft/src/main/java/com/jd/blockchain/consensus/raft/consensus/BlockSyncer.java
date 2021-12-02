package com.jd.blockchain.consensus.raft.consensus;

import com.jd.blockchain.ledger.ParticipantNode;

public interface BlockSyncer {

    ParticipantNode findParticipantNode();

    boolean sync(ParticipantNode node, long height) throws BlockSyncException;

}
