package com.jd.blockchain.consensus.raft.consensus;

public interface BlockSerializer {

    byte[] serialize(Block block);

    Block deserialize(byte[] blockBytes);

}
