package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.crypto.HashDigest;

public class LedgerLRUCache implements LedgerCache {

  private final HashDigest ledgerHash;
  private final AdminCache adminCache;
  private final UserCache userCache;
  private final DataAccountCache dataAccountCache;
  private final ContractCache contractCache;
  private final EventAccountCache eventAccountCache;

  public LedgerLRUCache() {
    this(null);
  }

  public LedgerLRUCache(HashDigest ledgerHash) {
    this.ledgerHash = ledgerHash;
    this.adminCache = new AdminLRUCache();
    this.userCache = new UserLRUCache();
    this.dataAccountCache = new DataAccountLRUCache();
    this.contractCache = new ContractLRUCache();
    this.eventAccountCache = new EventAccountLRUCache();
  }

  @Override
  public HashDigest getLedgerHash() {
    return ledgerHash;
  }

  @Override
  public AdminCache getAdminCache() {
    return adminCache;
  }

  @Override
  public UserCache getUserCache() {
    return userCache;
  }

  @Override
  public DataAccountCache getDataAccountCache() {
    return dataAccountCache;
  }

  @Override
  public ContractCache getContractCache() {
    return contractCache;
  }

  @Override
  public EventAccountCache getEventAccountCache() {
    return eventAccountCache;
  }

  @Override
  public void clear() {
    adminCache.clear();
    userCache.clear();
    dataAccountCache.clear();
    contractCache.clear();
    eventAccountCache.clear();
  }
}
