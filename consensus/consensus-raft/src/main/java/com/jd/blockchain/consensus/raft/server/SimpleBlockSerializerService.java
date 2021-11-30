package com.jd.blockchain.consensus.raft.server;

import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBlockSerializerService implements BlockSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBlockSerializerService.class);
    private static final String LIST_BYTE_CLASS_NAME = "[B";

    @Override
    public byte[] serialize(Block block) {
        return new byte[0];
    }

    @Override
    public Block deserialize(byte[] blockBytes) {
        return null;
    }

//    @Override
//    public byte[] serialize(List<byte[]> txList) {
//        try {
//            return SerializerManager.getSerializer(SerializerManager.Hessian2).serialize(txList);
//        } catch (CodecException e) {
//            LOGGER.error("serialize txs error", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public List<byte[]> deserialize(byte[] blockBytes) {
//        try {
//            return SerializerManager.getSerializer(SerializerManager.Hessian2).deserialize(blockBytes, LIST_BYTE_CLASS_NAME);
//        } catch (CodecException e) {
//            LOGGER.error("deserialize blockBytes error", e);
//            throw new RuntimeException(e);
//        }
//    }
}
