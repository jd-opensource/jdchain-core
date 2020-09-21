package com.jd.blockchain.storage.service.impl.kvdb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;

import com.jd.blockchain.kvdb.client.KVDBClient;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.storage.service.ExPolicy;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;

public class KVDBExPolicyStorage implements ExPolicyKVStorage {

    private Map<Bytes, Boolean> existss = new LRUMap<>(1024 * 128);
    private Map<Bytes, Boolean> batchExists;

    private static Bytes KEY_PREFIX = Bytes.fromString("K");

    private KVDBClient db;

    protected static Bytes encodeKey(Bytes dataKey) {
        return KEY_PREFIX.concat(dataKey);
    }

    public KVDBExPolicyStorage(KVDBClient db) {
        this.db = db;
    }

    @Override
    public byte[] get(Bytes key) {
        try {
            Bytes data = db.get(encodeKey(key));
            if (null != data) {
                return data.toBytes();
            } else {
                return null;
            }
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb get error", e);
        }
    }

    @Override
    public boolean exist(Bytes key) {
        try {
            Boolean exists = null;
            if (null != batchExists) {
                exists = batchExists.get(key);
            }
            if (null != exists) {
                return exists;
            }
            exists = existss.get(key);
            if (null != exists) {
                return exists;
            }
            exists = db.exists(encodeKey(key));
            if (null != batchExists) {
                batchExists.put(key, exists);
            } else {
                existss.put(key, exists);
            }
            return exists;
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb exists error", e);
        }
    }

    @Override
    public boolean set(Bytes key, byte[] value, ExPolicy ex) {
        try {
            switch (ex) {
                case EXISTING:
                    if (exist(key)) {
                        if (db.put(encodeKey(key), new Bytes(value))) {
                            if (null != batchExists) {
                                batchExists.put(key, true);
                            } else {
                                existss.put(key, true);
                            }
                            return true;
                        }
                    }
                    return false;
                case NOT_EXISTING:
                    if (!exist(key)) {
                        if (db.put(encodeKey(key), new Bytes(value))) {
                            if (null != batchExists) {
                                batchExists.put(key, true);
                            } else {
                                existss.put(key, true);
                            }
                            return true;
                        }
                    }
                    return false;
                default:
                    throw new IllegalArgumentException("Unsupported ExPolicy[" + ex.toString() + "]!");
            }
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb set error", e);
        }
    }

    @Override
    public void batchBegin() {
        try {
            batchExists = null;
            if (db.batchBegin()) {
                batchExists = new HashMap<>();
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
                if (null != batchExists) {
                    Iterator<Map.Entry<Bytes, Boolean>> iterator = batchExists.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Bytes, Boolean> entry = iterator.next();
                        existss.put(entry.getKey(), entry.getValue());
                    }
                }
                batchExists = null;
            } else {
                batchExists = null;
                throw new IllegalStateException("kvdb batch commit error");
            }
        } catch (KVDBException e) {
            throw new IllegalStateException("kvdb batch commit error", e);
        }
    }
}