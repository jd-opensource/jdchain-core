package com.jd.blockchain.kvdb.rocksdb;

import com.jd.blockchain.kvdb.KVWriteBatch;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

class KVWriteClusterBatch implements KVWriteBatch {

    public static final Logger LOGGER = LoggerFactory.getLogger(KVWriteClusterBatch.class);

    private KVWritePartition[] writingPartitions;

    private Partitioner partitioner;

    KVWriteClusterBatch(KVWritePartition[] writingPartitions, Partitioner partitioner) {
        this.writingPartitions = writingPartitions;
        this.partitioner = partitioner;
    }

    @Override
    public void set(String key, String value) {
        set(BytesUtils.toBytes(key), BytesUtils.toBytes(value));
    }

    @Override
    public void set(byte[] key, byte[] value) {
        int pid = partitioner.partition(key);
        writingPartitions[pid].set(key, value);
    }

    @Override
    public void commit() {
        // TODO: 非线程安全，需要修正；
        CountDownLatch completedLatch = new CountDownLatch(writingPartitions.length);
        for (KVWritePartition parti : writingPartitions) {
            parti.commit(completedLatch);
        }
        try {
            completedLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("KVWrite batch commit error! --" + e.getMessage(), e);
        }
    }

}