package com.jd.blockchain.consensus.bftsmart.client;

import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;

import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.reconfiguration.views.MemoryBasedViewStorage;
import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.AsynchServiceProxy;
import utils.serialize.binary.BinarySerializeUtils;

public class BftsmartPeerProxyFactory extends BasePooledObjectFactory<AsynchServiceProxy> {
	private static Logger LOGGER = LoggerFactory.getLogger(BftsmartPeerProxyFactory.class);
	private BftsmartClientSettings bftsmartClientSettings;

	private int idBase;
	private int idRange;
	
	/**
	 * 已分配 ID 的标记；值在 0 ~ {@link BftsmartSessionCredential#getClientIdRange()} 之间；
	 */
	private BitSet allocatedRange;

	public BftsmartPeerProxyFactory(BftsmartClientSettings bftsmartClientSettings) {
		this.bftsmartClientSettings = bftsmartClientSettings;
		this.idBase = bftsmartClientSettings.getSessionCredential().getClientId();
		this.idRange = bftsmartClientSettings.getSessionCredential().getClientIdRange();
		this.allocatedRange = new BitSet(idRange);
	}

	private synchronized int allocateId() {
		for (int i = 0; i < idRange; i++) {
			if (!allocatedRange.get(i)) {
				allocatedRange.set(i);
				return idBase + i;
			}
		}
		throw new IllegalStateException("Id allocation is overflow!");
	}
	
	private synchronized void recycleId(int id) {
		int r = id - idBase;
		if (r > -1 && r < idRange) {
			allocatedRange.clear(r);
		}
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
		int processId = allocateId();
		tomConfiguration.setProcessId(processId);
		AsynchServiceProxy peerProxy = new AsynchServiceProxy(tomConfiguration, viewStorage, bftsmartClientSettings.getSSLSecurity());

		if (LOGGER.isInfoEnabled()) {
			// 打印view
			int[] processes = view.getProcesses();
			NodeNetwork[] addresses = new NodeNetwork[processes.length];
			for (int i = 0; i < addresses.length; i++) {
				addresses[i] = view.getAddress(processes[i]);
			}
			LOGGER.info(
					"Creating pooled bftsmart client ... [PooledClientID={}] [ViewID={}] [ViewTopology={}] [Peers={}]",
					processId, view.getId(), Arrays.toString(processes), Arrays.toString(addresses));
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
			recycleId(serviceProxy.getProcessId());
			serviceProxy.close();
		}
	}
}
