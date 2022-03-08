package com.jd.blockchain.consensus.raft.server;

import com.google.common.collect.Lists;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockProposer;
import com.jd.blockchain.consensus.raft.consensus.BlockSerializer;
import junit.framework.TestCase;
import org.junit.Assert;

import static com.jd.blockchain.consensus.raft.server.BlockProposerServiceTest.mockLedgerRepository;

public class SimpleBlockSerializerServiceTest extends TestCase {

    private BlockSerializer blockSerializer = new SimpleBlockSerializerService();
    private BlockProposer blockProposer = new BlockProposerService(mockLedgerRepository);

    public void testSerialize() {

        Block block = blockProposer.proposeBlock(Lists.newArrayList(new byte[]{1, 2}));

        byte[] serialize = blockSerializer.serialize(block);

        Block block1 = blockSerializer.deserialize(serialize);

        Assert.assertEquals(block.getPreBlockHash(), block1.getPreBlockHash());


    }

}