package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.crypto.HashDigest;

/**
 * 账本相关缓存服务
 */
public interface LedgerCache extends Clearable {

    /**
     * 账本哈希
     *
     * @return
     */
    HashDigest getLedgerHash();

    /**
     * 管理相关缓存
     *
     * @return
     */
    AdminCache getAdminCache();

    /**
     * 用户数据缓存
     *
     * @return
     */
    UserCache getUserCache();

    /**
     * 数据账户缓存
     *
     * @return
     */
    DataAccountCache getDataAccountCache();

    /**
     * 合约数据缓存
     *
     * @return
     */
    ContractCache getContractCache();

    /**
     * 事件数据缓存
     *
     * @return
     */
    EventAccountCache getEventAccountCache();
}
