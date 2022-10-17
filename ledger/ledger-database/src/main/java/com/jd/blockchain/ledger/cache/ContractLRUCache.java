package com.jd.blockchain.ledger.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.ledger.DataPermission;
import utils.Bytes;

public class ContractLRUCache implements ContractCache {

    private final Cache<Bytes, PubKey> pubKeyCache;
    private final Cache<Bytes, AccountState> stateCache;
    private final Cache<Bytes, DataPermission> permissionCache;
    private final Cache<Bytes, ContractCode> contractCodeCache;

    public ContractLRUCache() {
        this.pubKeyCache =
                CacheBuilder.newBuilder().initialCapacity(1).maximumSize(100).concurrencyLevel(1).build();
        this.stateCache =
                CacheBuilder.newBuilder().initialCapacity(1).maximumSize(100).concurrencyLevel(1).build();
        this.permissionCache =
                CacheBuilder.newBuilder().initialCapacity(1).maximumSize(100).concurrencyLevel(1).build();
        this.contractCodeCache =
                CacheBuilder.newBuilder().initialCapacity(1).maximumSize(20).concurrencyLevel(1).build();
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
    public AccountState getState(Bytes address) {
        return stateCache.getIfPresent(address);
    }

    @Override
    public void setState(Bytes address, AccountState state) {
        stateCache.put(address, state);
    }

    @Override
    public ContractCode getContractCode(Bytes address, long version) {
        return contractCodeCache.getIfPresent(cacheKey(address, version));
    }

    @Override
    public void setContractCode(Bytes address, ContractCode contractCode) {
        contractCodeCache.put(cacheKey(address, contractCode.getVersion()), contractCode);
    }

    private Bytes cacheKey(Bytes address, long version) {
        return address.concat(Bytes.fromLong(version));
    }

    @Override
    public void clear() {
        pubKeyCache.invalidateAll();
        stateCache.invalidateAll();
        permissionCache.invalidateAll();
        contractCodeCache.invalidateAll();
    }
}
