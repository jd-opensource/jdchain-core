package com.jd.blockchain.consensus.bftsmart.client;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;
import com.jd.blockchain.utils.serialize.binary.BinarySerializeUtils;

import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.reconfiguration.views.MemoryBasedViewStorage;
import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.AsynchServiceProxy;

public class BftsmartPeerProxyFactory extends BasePooledObjectFactory<AsynchServiceProxy> {
	private static Logger LOGGER = LoggerFactory.getLogger(BftsmartPeerProxyFactory.class);
	private BftsmartClientSettings bftsmartClientSettings;

	private int gatewayId;

	private AtomicInteger index = new AtomicInteger(1);

	public BftsmartPeerProxyFactory(BftsmartClientSettings bftsmartClientSettings, int gatewayId) {
		this.bftsmartClientSettings = bftsmartClientSettings;
		this.gatewayId = gatewayId;
	}

	@Override
	public AsynchServiceProxy create() throws Exception {
		BftsmartTopology topology = BinarySerializeUtils.deserialize(bftsmartClientSettings.getTopology());

		View view = topology.getView();
		if (view == null) {
			throw new IllegalStateException("No topology view in the bftsmart client settings!");
		}

		MemoryBasedViewStorage viewStorage = new MemoryBasedViewStorage(view);
		TOMConfiguration tomConfiguration = BinarySerializeUtils.deserialize(bftsmartClientSettings.getTomConfig());

		// every proxy client has unique id;
		int pooledClientId = gatewayId + index.getAndIncrement();
		tomConfiguration.setProcessId(pooledClientId);
		AsynchServiceProxy peerProxy = new AsynchServiceProxy(tomConfiguration, viewStorage);

		if (LOGGER.isDebugEnabled()) {
			// 打印view
			int[] processes = view.getProcesses();
			NodeNetwork[] addresses = new NodeNetwork[processes.length];
			for (int i = 0; i < addresses.length; i++) {
				addresses[i] = view.getAddress(processes[i]);
			}
			LOGGER.debug(
					"Creating pooled bftsmart client ... [PooledClientID={}] [ViewID={}] [ViewTopology={}] [Peers={}]",
					pooledClientId, view.getId(), Arrays.toString(processes), Arrays.toString(addresses));
		}

		return peerProxy;
	}
	
	@Override
	public boolean validateObject(PooledObject<AsynchServiceProxy> p) {
		// TODO Auto-generated method stub
		return super.validateObject(p);
	}
	
	@Override
	public void activateObject(PooledObject<AsynchServiceProxy> p) throws Exception {
		// TODO Auto-generated method stub
		super.activateObject(p);
	}
	
	@Override
	public void passivateObject(PooledObject<AsynchServiceProxy> p) throws Exception {
		// TODO Auto-generated method stub
		super.passivateObject(p);
	}

	@Override
	public PooledObject<AsynchServiceProxy> wrap(AsynchServiceProxy asynchServiceProxy) {
		return new DefaultPooledObject<>(asynchServiceProxy);
	}

	// when close pool, destroy its object
	@Override
	public void destroyObject(PooledObject<AsynchServiceProxy> p) throws Exception {
		super.destroyObject(p);
		AsynchServiceProxy serviceProxy = p.getObject();
		if (serviceProxy != null) {
			serviceProxy.close();
		}
	}
}
