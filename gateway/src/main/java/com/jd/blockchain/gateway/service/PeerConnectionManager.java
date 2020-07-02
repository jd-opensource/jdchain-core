package com.jd.blockchain.gateway.service;

import javax.annotation.PreDestroy;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.gateway.event.EventListener;
import com.jd.blockchain.gateway.event.EventListenerService;
import com.jd.blockchain.gateway.event.PullEventListener;
import org.springframework.stereotype.Component;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.gateway.PeerConnector;
import com.jd.blockchain.gateway.PeerService;
import com.jd.blockchain.sdk.service.PeerBlockchainServiceFactory;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.TransactionService;
import com.jd.blockchain.utils.net.NetworkAddress;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class PeerConnectionManager implements PeerService, PeerConnector, EventListenerService {

	private final Set<HashDigest> localLedgerCache = new HashSet<>();

	private final Lock ledgerHashLock = new ReentrantLock();

	private volatile PeerBlockchainServiceFactory peerServiceFactory;

	private volatile NetworkAddress peerAddress;

	private volatile AsymmetricKeypair gateWayKeyPair;

	private volatile List<String> peerProviders;

	private volatile EventListener eventListener;

	@Override
	public NetworkAddress getPeerAddress() {
		return peerAddress;
	}

	@Override
	public boolean isConnected() {
		return peerServiceFactory != null;
	}

	@Override
	public synchronized void connect(NetworkAddress peerAddress, AsymmetricKeypair defaultKeyPair, List<String> peerProviders) {
		if (isConnected()) {
			if (this.peerAddress.equals(peerAddress)) {
				return;
			}
			throw new IllegalArgumentException(
					"This gateway has been connected to a peer, cann't be connected to another peer before closing it!");
		}
		setPeerAddress(peerAddress);
		setGateWayKeyPair(defaultKeyPair);
		setPeerProviders(peerProviders);
		// 运行时出错时由定时任务动态重连；
		peerServiceFactory = PeerBlockchainServiceFactory.connect(defaultKeyPair, peerAddress, peerProviders);
		// 连接成功的话，更新账本
		ledgerHashLock.lock();
		try {
			updateLedgerCache();
		} finally {
			ledgerHashLock.unlock();
		}
	}

	@Override
	public void monitorAndReconnect() {
		if (getPeerAddress() == null) {
			throw new IllegalArgumentException("Peer address must be init first !!!");
		}
		/**
		 * 1、首先判断是否之前连接成功过，若未成功则重连，走auth逻辑
		 * 2、若成功，则判断对端节点的账本与当前账本是否一致，有新增的情况下重连
		 */
		ledgerHashLock.lock();
		try {
			if (isConnected()) {
				// 已连接成功，判断账本信息
				HashDigest[] peerLedgerHashs = getQueryService().getLedgerHashs();
				if (peerLedgerHashs != null && peerLedgerHashs.length > 0) {
					boolean haveNewLedger = false;
					for (HashDigest hash : peerLedgerHashs) {
						if (!localLedgerCache.contains(hash)) {
							haveNewLedger = true;
							break;
						}
					}
					if (haveNewLedger) {
						// 有新账本的情况下重连，并更新本地账本
						peerServiceFactory = PeerBlockchainServiceFactory.connect(gateWayKeyPair, peerAddress, peerProviders);
						localLedgerCache.addAll(Arrays.asList(peerLedgerHashs));
					}
				}
			} else {
				peerServiceFactory = PeerBlockchainServiceFactory.connect(gateWayKeyPair, peerAddress, peerProviders);
				updateLedgerCache();
			}
		} finally {
			ledgerHashLock.unlock();
		}
	}

	@Override
	public void close() {
		PeerBlockchainServiceFactory serviceFactory = this.peerServiceFactory;
		if (serviceFactory != null) {
			this.peerServiceFactory = null;
			this.peerAddress = null;
			serviceFactory.close();
		}
	}

	@Override
	public BlockchainQueryService getQueryService() {
		PeerBlockchainServiceFactory serviceFactory = this.peerServiceFactory;
		if (serviceFactory == null) {
			throw new IllegalStateException("Peer connection was closed!");
		}
		return serviceFactory.getBlockchainService();
	}

	@Override
	public TransactionService getTransactionService() {
		PeerBlockchainServiceFactory serviceFactory = this.peerServiceFactory;
		if (serviceFactory == null) {
			throw new IllegalStateException("Peer connection was closed!");
		}

		return serviceFactory.getTransactionService();
	}

	@PreDestroy
	private void destroy() {
		close();
	}

	public void setPeerAddress(NetworkAddress peerAddress) {
		this.peerAddress = peerAddress;
	}

	public void setGateWayKeyPair(AsymmetricKeypair gateWayKeyPair) {
		this.gateWayKeyPair = gateWayKeyPair;
	}

	public void setPeerProviders(List<String> peerProviders) {
		this.peerProviders = peerProviders;
	}

	@Override
	public EventListener getEventListener() {
		if (eventListener == null) {
			eventListener = new PullEventListener(getQueryService());
			eventListener.start();
		}
		return eventListener;
	}

	/**
	 * 更新本地账本缓存
	 */
	private void updateLedgerCache() {
		if (isConnected()) {
			HashDigest[] peerLedgerHashs = getQueryService().getLedgerHashs();
			if (peerLedgerHashs != null && peerLedgerHashs.length > 0) {
				localLedgerCache.addAll(Arrays.asList(peerLedgerHashs));
			}
		}
	}
}
