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
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import bftsmart.tom.AsynchServiceProxy;

public class BftsmartServiceProxyPool extends GenericObjectPool<AsynchServiceProxy> {

	public BftsmartServiceProxyPool(BftsmartClientSettings clientSettings) {
		super(new BftsmartPeerProxyFactory(clientSettings));
		init(clientSettings);
	}

	public BftsmartServiceProxyPool(BftsmartClientSettings clientSettings, int maxTotal, int minIdle,
			int maxIdle) {
		super(new BftsmartPeerProxyFactory(clientSettings));
		init(clientSettings);
	}
	
	private void init(BftsmartClientSettings clientSettings) {
		GenericObjectPoolConfig<AsynchServiceProxy> poolConfig = new GenericObjectPoolConfig<>();
		int maxTotal = clientSettings.getCredentialInfo().getClientIdRange();
		poolConfig.setMaxTotal(maxTotal);
		poolConfig.setMaxIdle(maxTotal);
		poolConfig.setMinIdle(0);
		
		setConfig(poolConfig);
	}
	
}