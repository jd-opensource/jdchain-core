package com.jd.blockchain.ledger.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.AccountState;
import utils.Bytes;

public class UserLRUCache implements UserCache {

  private final Cache<Bytes, PubKey> pubKeyCache;
  private final Cache<Bytes, AccountState> stateCache;
  private final Cache<Bytes, String> certCache;

  public UserLRUCache() {
    this.pubKeyCache =
        CacheBuilder.newBuilder().initialCapacity(4).maximumSize(10).concurrencyLevel(1).build();
    this.stateCache =
        CacheBuilder.newBuilder().initialCapacity(4).maximumSize(10).concurrencyLevel(1).build();
    this.certCache =
        CacheBuilder.newBuilder().initialCapacity(4).maximumSize(10).concurrencyLevel(1).build();
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
  public AccountState getState(Bytes address) {
    return stateCache.getIfPresent(address);
  }

  @Override
  public void setState(Bytes address, AccountState state) {
    stateCache.put(address, state);
  }

  @Override
  public String getCertificate(Bytes address) {
    return certCache.getIfPresent(address);
  }

  @Override
  public void setCertificate(Bytes address, String cert) {
    certCache.put(address, cert);
  }

  @Override
  public void clear() {
    pubKeyCache.invalidateAll();
    stateCache.invalidateAll();
    certCache.invalidateAll();
  }
}
