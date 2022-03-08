package com.jd.blockchain.consensus.raft.server;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBlockSerializerService implements BlockSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBlockSerializerService.class);

    @Override
    public byte[] serialize(Block block) {
        try {
            return SerializerManager.getSerializer(SerializerManager.Hessian2).serialize(block);
        } catch (CodecException e) {
            LOGGER.error("serialize block error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Block deserialize(byte[] blockBytes) {
        try {
            return SerializerManager.getSerializer(SerializerManager.Hessian2).deserialize(blockBytes, Block.class.getName());
        } catch (CodecException e) {
            LOGGER.error("deserialize blockBytes error", e);
            throw new RuntimeException(e);
        }
    }
}
