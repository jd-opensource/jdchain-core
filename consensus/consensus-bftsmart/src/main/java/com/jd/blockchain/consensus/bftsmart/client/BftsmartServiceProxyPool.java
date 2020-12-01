/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.bft.BftsmartConsensusClientPool
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/10/30 下午6:50
 * Description:
 */
package com.jd.blockchain.consensus.bftsmart.client;

import org.apache.commons.pool2.impl.GenericObjectPool;

import bftsmart.tom.AsynchServiceProxy;

public class BftsmartServiceProxyPool extends GenericObjectPool<AsynchServiceProxy> {

	public BftsmartServiceProxyPool(int gatewayId, BftsmartClientSettings clientSettings) {
		super(new BftsmartPeerProxyFactory((BftsmartClientSettings) clientSettings, gatewayId),
				new BftsmartPeerProxyPoolConfig());
	}

	public BftsmartServiceProxyPool(int gatewayId, BftsmartClientSettings clientSettings, int maxTotal, int minIdle,
			int maxIdle) {
		super(new BftsmartPeerProxyFactory((BftsmartClientSettings) clientSettings, gatewayId),
				new BftsmartPeerProxyPoolConfig(maxTotal, minIdle, maxIdle));
	}
}