package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.ledger.AccountState;
import utils.Bytes;

/** 账户类状态缓存 */
public interface StateCache {

  AccountState getState(Bytes address);

  void setState(Bytes address, AccountState state);
}
