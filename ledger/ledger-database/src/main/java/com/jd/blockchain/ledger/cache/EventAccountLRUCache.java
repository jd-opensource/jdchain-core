package com.jd.blockchain.ledger.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.DataPermission;
import utils.Bytes;

public class EventAccountLRUCache implements EventAccountCache {

    private final Cache<Bytes, PubKey> pubKeyCache;
    private final Cache<Bytes, DataPermission> permissionCache;

    public EventAccountLRUCache() {
        this.pubKeyCache =
                CacheBuilder.newBuilder().initialCapacity(1).maximumSize(100).concurrencyLevel(1).build();
        this.permissionCache =
                CacheBuilder.newBuilder().initialCapacity(1).maximumSize(100).concurrencyLevel(1).build();
    }

    @Override
    public PubKey getPubkey(Bytes address) {
        return pubKeyCache.getIfPresent(address);
    }

    @Override
    public void setPubkey(Bytes address, PubKey pubKey) {
        pubKeyCache.put(address, pubKey);
    }

    @Override
    public DataPermission getPermission(Bytes address) {
        return permissionCache.getIfPresent(address);
    }

    @Override
    public void setPermission(Bytes address, DataPermission permission) {
        permissionCache.put(address, permission);
    }

    @Override
    public void clear() {
        pubKeyCache.invalidateAll();
        permissionCache.invalidateAll();
    }
}
