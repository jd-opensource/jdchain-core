package com.jd.blockchain.consensus.raft.consensus;

import java.util.List;

public interface BlockSerializer {

    byte[] serialize(Block block);

    Block deserialize(byte[] blockBytes);

}
