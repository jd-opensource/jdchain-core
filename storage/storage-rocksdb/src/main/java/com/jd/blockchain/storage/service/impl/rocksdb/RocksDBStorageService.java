package com.jd.blockchain.storage.service.impl.rocksdb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.jd.blockchain.storage.service.CacheConfig;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.KVStorageService;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import utils.Bytes;

public class RocksDBStorageService implements KVStorageService {

    private ExPolicyKVStorage exStorage;

    private VersioningKVStorage verStorage;

    public RocksDBStorageService(RocksDB db, CacheConfig cacheConfig) {
        BloomFilter<byte[]> bloomFilter = null;
        if (cacheConfig.getBloomConfig().isEnable()) {
            bloomFilter = BloomFilter.create(
                    Funnels.byteArrayFunnel(),
                    cacheConfig.getBloomConfig().getExpectedInsertions(),
                    cacheConfig.getBloomConfig().getFpp());
            try (RocksIterator iterator = db.newIterator()) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    bloomFilter.put(iterator.key());
                }
            } catch (Exception e) {
                bloomFilter = null;
            }
        }
        Cache<Bytes, byte[]> lruCache = null;
        if (cacheConfig.getLruCacheConfig().isEnable()) {
            lruCache = CacheBuilder.newBuilder()
                    .initialCapacity(cacheConfig.getLruCacheConfig().getInitialCapacity())
                    .maximumSize(cacheConfig.getLruCacheConfig().getMaximumSize())
                    .build();
        }
        this.verStorage = new RocksDBVersioningStorage(db, bloomFilter, lruCache);
        this.exStorage = new RocksDBExPolicyStorage(db, bloomFilter, lruCache);
    }

    @Override
    public ExPolicyKVStorage getExPolicyKVStorage() {
        return exStorage;
    }

    @Override
    public VersioningKVStorage getVersioningKVStorage() {
        return verStorage;
    }

}
