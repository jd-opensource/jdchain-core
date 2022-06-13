package com.jd.blockchain.storage.service.impl.rocksdb;

import com.google.common.cache.Cache;
import com.google.common.hash.BloomFilter;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import org.rocksdb.*;
import utils.Bytes;
import utils.DataEntry;
import utils.io.BytesUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RocksDBVersioningStorage implements VersioningKVStorage {

    private static Bytes VERSION_PREFIX = Bytes.fromString("V");
    private static Bytes DATA_PREFIX = Bytes.fromString("D");
    private final WriteOptions writeOptions = new WriteOptions();
    private final ReadOptions readOptions = new ReadOptions().setFillCache(true).setVerifyChecksums(false);
    private final Cache<Bytes, byte[]> cache;
    private Map<Bytes, Long> batchVersions;
    private Map<Bytes, byte[]> batchKVs;
    private RocksDB db;
    private WriteBatch writeBatch;
    private BloomFilter<byte[]> bloomFilter;

    public RocksDBVersioningStorage(RocksDB db, BloomFilter<byte[]> bloomFilter, Cache<Bytes, byte[]> cache) {
        this.db = db;
        this.bloomFilter = bloomFilter;
        this.cache = cache;
        if (null != bloomFilter || null != cache) {
            batchKVs = new HashMap<>();
            batchVersions = new HashMap<>();
        }
    }

    protected static Bytes encodeVersionKey(Bytes dataKey) {
        return VERSION_PREFIX.concat(dataKey);
    }

    protected static Bytes encodeDataKey(Bytes dataKey, long version) {
        return DATA_PREFIX.concat(dataKey).concat(Bytes.fromLong(version));
    }

    @Override
    public long getVersion(Bytes key) {
        try {
            Bytes vkb = encodeVersionKey(key);
            byte[] vkbs = vkb.toBytes();
            byte[] vBytes = null;
            if (null != cache) {
                vBytes = cache.getIfPresent(vkb);
            }
            if (null != vBytes) {
                return BytesUtils.toLong(vBytes);
            }
            if (null != bloomFilter && !bloomFilter.mightContain(vkbs)) {
                return -1;
            }
            vBytes = db.get(readOptions, vkbs);
            if (null != vBytes) {
                if (null != cache) {
                    cache.put(vkb, vBytes);
                }
                return BytesUtils.toLong(vBytes);
            }
            return -1;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb get version error", e);
        }
    }

    @Override
    public DataEntry<Bytes, byte[]> getEntry(Bytes key, long version) {
        byte[] value = get(key, version);
        if (value == null) {
            return null;
        }
        return new VersioningKVData(key, version, value);
    }

    @Override
    public byte[] get(Bytes key, long version) {
        try {
            long latestVersion = getVersion(key);
            if (latestVersion < 0) {
                return null;
            }
            if (version > latestVersion) {
                return null;
            }
            version = version < 0 ? latestVersion : version;
            Bytes dkb = encodeDataKey(key, version);
            byte[] bytes = null;
            if (null != cache) {
                bytes = cache.getIfPresent(dkb);
            }
            if (null != bytes) {
                return bytes;
            }
            byte[] dkbs = dkb.toBytes();
            if (null != bloomFilter && !bloomFilter.mightContain(dkbs)) {
                return null;
            }
            bytes = this.db.get(readOptions, dkbs);
            if (null == bytes) {
            } else if (null != cache) {
                cache.put(dkb, bytes);
            }
            return bytes;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb get error", e);
        }
    }

    @Override
    public long set(Bytes key, byte[] value, long version) {
        long v = version + 1;
        try {
            Bytes dkb = encodeDataKey(key, v);
            byte[] dkbs = dkb.toBytes();
            Bytes vkb = encodeVersionKey(key);
            byte[] vkbs = vkb.toBytes();
            byte[] vbs = BytesUtils.toBytes(v);
            if (null != writeBatch) {
                writeBatch.put(vkbs, vbs);
                writeBatch.put(dkbs, value);
                if (null != batchVersions) {
                    batchVersions.put(vkb, v);
                }
                if (null != batchKVs) {
                    batchKVs.put(dkb, value);
                }
            } else {
                db.put(vkbs, vbs);
                db.put(dkbs, value);
                if (null != bloomFilter) {
                    bloomFilter.put(vkbs);
                    bloomFilter.put(dkbs);
                }
                if (null != cache) {
                    cache.put(vkb, vbs);
                    cache.put(dkb, value);
                }
            }

            return v;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb set error", e);
        }
    }

    @Override
    public void batchBegin() {
        writeBatch = new WriteBatch();
        if (null != batchVersions) {
            batchVersions.clear();
        }
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
            if (null != batchVersions && batchVersions.size() > 0) {
                Iterator<Map.Entry<Bytes, Long>> iterator = batchVersions.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Bytes, Long> entry = iterator.next();
                    if (null != bloomFilter) {
                        bloomFilter.put(entry.getKey().toBytes());
                    }
                    if (null != cache) {
                        cache.put(entry.getKey(), BytesUtils.toBytes(entry.getValue()));
                    }
                }
            }
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
            if (null != batchVersions) {
                batchVersions.clear();
            }
        }
    }


    private static class VersioningKVData implements DataEntry {

        private Bytes key;

        private long version;

        private byte[] value;

        public VersioningKVData(Bytes key, long version, byte[] value) {
            this.key = key;
            this.version = version;
            this.value = value;
        }

        @Override
        public Bytes getKey() {
            return key;
        }

        @Override
        public long getVersion() {
            return version;
        }

        @Override
        public byte[] getValue() {
            return value;
        }
    }

    @Override
    public long archiveRemove(Bytes key, long version) {
        long v = version + 1;
        try {
            Bytes dkb = encodeDataKey(key, v);
            byte[] dkbs = dkb.toBytes();
            db.delete(dkbs);

            // 考虑一下是否需要同时从cache/bloomFilter中删除
            return 0;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb archiveRemove error", e);
        }
    }

    @Override
    public long archiveSet(Bytes key, byte[] value, long version) {
        long v = version + 1;
        try {
            Bytes dkb = encodeDataKey(key, v);
            byte[] dkbs = dkb.toBytes();

            if (null != writeBatch) {
                writeBatch.put(dkbs, value);
                if (null != batchKVs) {
                    batchKVs.put(dkb, value);
                }
            } else {
                db.put(dkbs, value);
                if (null != bloomFilter) {
                    bloomFilter.put(dkbs);
                }
                if (null != cache) {
                    cache.put(dkb, value);
                }
            }

            return 0;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb archiveSet error", e);
        }
    }

    @Override
    public byte[] archiveGet(Bytes key, long version) {
        try {
            if (version < 0) {
                return null;
            }

            Bytes dkb = encodeDataKey(key, version);
            byte[] bytes = null;
            if (null != cache) {
                bytes = cache.getIfPresent(dkb);
            }
            if (null != bytes) {
                return bytes;
            }
            byte[] dkbs = dkb.toBytes();
            if (null != bloomFilter && !bloomFilter.mightContain(dkbs)) {
                return null;
            }
            bytes = this.db.get(readOptions, dkbs);
            if (null == bytes) {
            } else if (null != cache) {
                cache.put(dkb, bytes);
            }
            return bytes;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb archiveGet error", e);
        }
    }

    @Override
    public void iterateAllKeys() {
        try {
            RocksIterator rocksIterator = db.newIterator();
            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                byte[] key = rocksIterator.key();
                byte[] value = db.get(key);
                String s = new String(key);
                System.out.println("key: " + s);
                System.out.println("value: " + Arrays.toString(value));
            }
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb iterateAllKeys error", e);
        }
    }
}