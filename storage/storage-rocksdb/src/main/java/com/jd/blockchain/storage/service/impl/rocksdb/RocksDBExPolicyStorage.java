package com.jd.blockchain.storage.service.impl.rocksdb;

import com.google.common.cache.Cache;
import com.google.common.hash.BloomFilter;
import com.jd.blockchain.storage.service.ExPolicy;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import org.rocksdb.*;
import utils.Bytes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RocksDBExPolicyStorage implements ExPolicyKVStorage {

    private static final Bytes KEY_PREFIX = Bytes.fromString("D");
    private final WriteOptions writeOptions = new WriteOptions();
    private final ReadOptions readOptions = new ReadOptions().setFillCache(true).setVerifyChecksums(false);
    private final RocksDB db;
    private final BloomFilter<byte[]> bloomFilter;
    private final Cache<Bytes, byte[]> cache;
    private Map<Bytes, byte[]> batchKVs;
    private WriteBatch writeBatch;

    public RocksDBExPolicyStorage(RocksDB db, BloomFilter<byte[]> bloomFilter, Cache<Bytes, byte[]> cache) {
        this.db = db;
        this.bloomFilter = bloomFilter;
        this.cache = cache;
        if (null != bloomFilter || null != cache) {
            batchKVs = new HashMap<>();
        }
    }

    protected static Bytes encodeDataKey(Bytes dataKey, long version) {
        return KEY_PREFIX.concat(dataKey).concat(Bytes.fromLong(version));
    }

    @Override
    public byte[] get(Bytes key) {
        try {
            Bytes kb = encodeDataKey(key, 0);
            byte[] kbs = kb.toBytes();
            byte[] bytes = null;
            if (null != cache) {
                bytes = cache.getIfPresent(kb);
            }
            if (null != bytes) {
                return bytes;
            }
            if (null != bloomFilter && !bloomFilter.mightContain(kbs)) {
                return null;
            }
            bytes = db.get(readOptions, kbs);
            if (null != bytes && null != cache) {
                cache.put(kb, bytes);
            }
            return bytes;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb get error", e);
        }
    }

    @Override
    public boolean exist(Bytes key) {
        try {
            Bytes kb = encodeDataKey(key, 0);
            byte[] bytes = null;
            if (null != cache) {
                bytes = cache.getIfPresent(kb);
            }
            if (null != bytes) {
                return true;
            }
            byte[] kbs = kb.toBytes();
            if (null != bloomFilter && !bloomFilter.mightContain(kbs)) {
                return false;
            }
            bytes = db.get(readOptions, kbs);
            if (null != bytes && null != cache) {
                cache.put(kb, bytes);
            }
            return null != bytes;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb exists error", e);
        }
    }

    @Override
    public boolean set(Bytes key, byte[] value, ExPolicy ex) {
        try {
            switch (ex) {
                case EXISTING:
                    if (!exist(key)) {
                        return false;
                    }
                    set(key, value);
                    return true;
                case NOT_EXISTING:
                    if (exist(key)) {
                        return false;
                    }
                    set(key, value);
                    return true;
                default:
                    throw new IllegalArgumentException("Unsupported ExPolicy[" + ex.toString() + "]!");
            }
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb set error", e);
        }
    }

    private void set(Bytes key, byte[] value) throws RocksDBException {
        Bytes kb = encodeDataKey(key, 0);
        byte[] kbs = kb.toBytes();
        if (null != writeBatch) {
            writeBatch.put(kbs, value);
            if (null != batchKVs) {
                batchKVs.put(kb, value);
            }
        } else {
            db.put(kbs, value);
            if (null != bloomFilter) {
                bloomFilter.put(kbs);
            }
            if (null != cache) {
                cache.put(kb, value);
            }
        }
    }

    @Override
    public void batchBegin() {
        writeBatch = new WriteBatch();
        if (null != batchKVs) {
            batchKVs.clear();
        }
    }

    @Override
    public void batchCommit() {
        try {
            if (null == writeBatch) {
                return;
            }
            db.write(writeOptions, writeBatch);
            if (null != batchKVs && batchKVs.size() > 0) {
                Iterator<Map.Entry<Bytes, byte[]>> iterator = batchKVs.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Bytes, byte[]> entry = iterator.next();
                    if (null != bloomFilter) {
                        bloomFilter.put(entry.getKey().toBytes());
                    }
                    if (null != cache) {
                        cache.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("rocksdb batch commit error", e);
        } finally {
            writeBatch = null;
            if (null != batchKVs) {
                batchKVs.clear();
            }
        }
    }
}