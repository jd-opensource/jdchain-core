package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.crypto.PubKey;
import utils.Bytes;

/** 公钥缓存 */
public interface PubkeyCache {

  PubKey getPubkey(Bytes address);

  void setPubkey(Bytes address, PubKey pubKey);
}
