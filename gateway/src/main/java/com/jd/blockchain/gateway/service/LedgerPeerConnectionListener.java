package com.jd.blockchain.gateway.service;

import utils.net.NetworkAddress;

/**
 * 账本节点连接监听
 */
public interface LedgerPeerConnectionListener {

    /**
     * 成功连接到某个节点
     *
     * @param peer
     */
    void connected(NetworkAddress peer);

}
