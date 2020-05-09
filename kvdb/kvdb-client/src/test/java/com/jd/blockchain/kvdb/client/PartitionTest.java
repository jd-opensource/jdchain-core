package com.jd.blockchain.kvdb.client;

import com.jd.blockchain.kvdb.client.partition.Partitioner;
import com.jd.blockchain.kvdb.client.partition.SimpleMurmur3HashPartitioner;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class PartitionTest {

    @Test
    public void test() {
        int part = 3;
        Partitioner partitioner = new SimpleMurmur3HashPartitioner(part);

        byte[] k = "k".getBytes();
        int p1 = partitioner.partition(k);
        int p2 = partitioner.partition(k);
        Assert.assertEquals(p1, p2);

        int keySizes = 1000;
        int[] keysInParts = new int[part];
        for (int i = 0; i < keySizes; i++) {
            keysInParts[partitioner.partition(UUID.randomUUID().toString().getBytes())]++;
        }

        int sum = 0;
        for (int keysInPart : keysInParts) {
            Assert.assertTrue(keysInPart > 0);
            sum += keysInPart;
        }
        Assert.assertEquals(keySizes, sum);

    }
}
