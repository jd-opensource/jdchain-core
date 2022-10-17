package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.ledger.DataPermission;
import utils.Bytes;

/** 账户类数据权限缓存 */
public interface PermissionCache {

  DataPermission getPermission(Bytes address);

  void setPermission(Bytes address, DataPermission permission);
}
