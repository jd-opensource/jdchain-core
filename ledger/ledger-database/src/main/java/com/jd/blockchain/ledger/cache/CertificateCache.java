package com.jd.blockchain.ledger.cache;

import utils.Bytes;

/** 证书缓存 */
public interface CertificateCache {

  String getCertificate(Bytes address);

  void setCertificate(Bytes address, String cert);
}
