package com.jd.blockchain.kvdb.rocksdb;

import com.jd.blockchain.kvdb.KVWrite;
import com.jd.blockchain.kvdb.KVWriteBatch;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * 此对象需要确保线程安全；
 *
 * @author huanghaiquan
 */
class KVWritePartition implements KVWrite {

    public static final Logger LOGGER = LoggerFactory.getLogger(KVWritePartition.class);

    private ExecutorService executor;

    private KVWriteBatch batch;

    private List<KVItem> cache;

    public KVWritePartition(KVWriteBatch batch, ExecutorService executor) {
        this.batch = batch;
        this.executor = executor;
        this.cache = new ArrayList<>();
    }

    @Override
    public void set(String key, String value) {
        set(BytesUtils.toBytes(key), BytesUtils.toBytes(value));
    }

    @Override
    public void set(byte[] key, byte[] value) {
        cache.add(new KVItem(key, value));
    }

    public void commit(CountDownLatch completedLatch) {
        executor.submit(() -> {
            try {
                for (KVItem kvItem : cache) {
                    batch.set(kvItem.key, kvItem.value);
                }
                batch.commit();
            } catch (Exception e) {
                LOGGER.error("KVWrite batch task error! --" + e.getMessage(), e);
            } finally {
                cache.clear();
                completedLatch.countDown();
            }
        });
    }

    private static class KVItem {
        byte[] key;

        byte[] value;

        public KVItem(byte[] key, byte[] value) {
            this.key = key;
            this.value = value;
        }
    }
}