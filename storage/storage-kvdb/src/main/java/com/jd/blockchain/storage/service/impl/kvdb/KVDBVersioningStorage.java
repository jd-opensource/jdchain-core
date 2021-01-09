package com.jd.blockchain.storage.service.impl.kvdb;

import com.jd.blockchain.kvdb.client.KVDBClient;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.DataEntry;
import utils.io.BytesUtils;

import org.apache.commons.collections4.map.LRUMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class KVDBVersioningStorage implements VersioningKVStorage {

    private Map<Bytes, Long> versions = new LRUMap<>(1024 * 128);
    private Map<Bytes, Long> batchVersions;

    private static Bytes VERSION_PREFIX = Bytes.fromString("V");

    private static Bytes DATA_PREFIX = Bytes.fromString("D");

    private KVDBClient db;

    public KVDBVersioningStorage(KVDBClient db) {
        this.db = db;
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
            Long version = null;
            if (null != batchVersions) {
                version = batchVersions.get(key);
            }
            if (null != version) {
                return version;
            }
            version = versions.get(key);
            if (null != version) {
                return version;
            }
            Bytes vBytes = db.get(encodeVersionKey(key));
            if (null != vBytes) {
                version = BytesUtils.toLong(vBytes.toBytes());
                if (null != batchVersions) {
                    batchVersions.put(key, version);
                } else {
                    versions.put(key, version);
                }
                return version;
            }

            return -1;
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb get version error", e);
        }
    }

    @Override
    public DataEntry<Bytes, byte[]> getEntry(Bytes key, long version) {
        try {
            Bytes data = db.get(encodeDataKey(key, version));
            if (null != data) {
                return new VersioningKVData(key, version, data.toBytes());
            } else {
                return null;
            }
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb get entry error", e);
        }
    }

    @Override
    public byte[] get(Bytes key, long version) {
        try {
            return db.get(encodeDataKey(key, version)).toBytes();
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb get error", e);
        }
    }

    @Override
    public long set(Bytes key, byte[] value, long version) {
        long v = getVersion(key);
        if (v != version) {
            throw new IllegalStateException("kvdb version not match");
        } else {
            try {
                v = v + 1;
                boolean ok = db.put(encodeVersionKey(key), Bytes.fromLong(v));
                if (ok) {
                    ok = db.put(encodeDataKey(key, v), new Bytes(value));
                    if (ok) {
                        if (null != batchVersions) {
                            batchVersions.put(key, v);
                        } else {
                            versions.put(key, v);
                        }
                        return v;
                    } else {
                        return -1;
                    }
                } else {
                    return -1;
                }
            } catch (KVDBException e) {
                throw new IllegalStateException("kvdb set error", e);
            }
        }
    }

    @Override
    public void batchBegin() {
        try {
            batchVersions = null;
            if (db.batchBegin()) {
                batchVersions = new HashMap<>();
            } else {
                throw new IllegalStateException("kvdb batch begin error");
            }
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb batch begin error", e);
        }
    }

    @Override
    public void batchCommit() {
        try {
            if (db.batchCommit()) {
                if (null != batchVersions) {
                    Iterator<Map.Entry<Bytes, Long>> iterator = batchVersions.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Bytes, Long> entry = iterator.next();
                        versions.put(entry.getKey(), entry.getValue());
                    }
                }
                batchVersions = null;
            } else {
                batchVersions = null;
                throw new IllegalStateException("kvdb batch commit error");
            }
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb batch commit error", e);
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
}