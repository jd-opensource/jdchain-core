package com.jd.blockchain.kvdb.client.partition;

import com.jd.blockchain.utils.hash.MurmurHash3;

public class SimpleMurmur3HashPartitioner implements Partitioner {

    private static final int HASH_SEED = 256;

    private int partitionCount;

    public SimpleMurmur3HashPartitioner(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    @Override
    public int getPartitionCount() {
        return partitionCount;
    }

    @Override
    public int partition(byte[] key) {
        if (partitionCount == 1) {
            return 0;
        }
        int hash = MurmurHash3.murmurhash3_x86_32(key, 0, key.length, HASH_SEED);
        return partition(hash, partitionCount);
    }

    private static int partition(int hash, int partitionCount) {
        int idx = hash % partitionCount;
        return idx < 0 ? idx * -1 : idx;
    }
}
