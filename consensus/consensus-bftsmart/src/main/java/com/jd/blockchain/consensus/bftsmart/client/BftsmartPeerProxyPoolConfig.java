/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.bft.BftsmartConsensusClientPool
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/10/30 下午6:50
 * Description:
 */
package com.jd.blockchain.consensus.bftsmart.client;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.jd.blockchain.consensus.bftsmart.service.BftsmartClientAuthencationService;

import bftsmart.tom.AsynchServiceProxy;

public class BftsmartPeerProxyPoolConfig extends GenericObjectPoolConfig<AsynchServiceProxy> {

//    private int MAX_TOTAL = BftsmartClientAuthencationService.POOL_SIZE_PEER_CLIENT;
    private int MAX_TOTAL = 30;

    private int MIN_IDLE = 0;

    private int MAX_IDLE = MAX_TOTAL;

    public BftsmartPeerProxyPoolConfig() {
        setMaxTotal(MAX_TOTAL);
        setMinIdle(MIN_IDLE);
        setMaxIdle(MAX_IDLE);
    }

    public BftsmartPeerProxyPoolConfig(int maxTotal, int minIdle, int maxIdle) {
        setMaxTotal(maxTotal);
        setMinIdle(minIdle);
        setMaxIdle(maxIdle);
    }
}