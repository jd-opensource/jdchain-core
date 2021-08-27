package com.jd.blockchain.gateway.service;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import utils.net.NetworkAddress;

import java.util.Set;

/**
 * 节点账本变化监听
 */
public interface LedgersListener {

    /**
     * 最新账本列表
     *
     * @param ledgers 账本列表
     * @param keyPair 准入身份
     * @param peer    账本检测节点地址
     */
    void LedgersUpdated(Set<HashDigest> ledgers, AsymmetricKeypair keyPair, NetworkAddress peer);

    /**
     * 移除账本
     *
     * @param ledger
     */
    void LedgerRemoved(HashDigest ledger);
}
