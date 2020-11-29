package com.jd.blockchain.consensus.bftsmart.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jd.blockchain.consensus.ClientIdentification;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingConfig;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.utils.serialize.binary.BinarySerializeUtils;

public class BftsmartClientAuthencationService implements ClientAuthencationService {

	public static final int MAX_CLIENT_COUNT = 20000;

	public static final int POOL_SIZE_PEER_CLIENT = 100;
	
	private BftsmartNodeServer nodeServer;

	private AtomicInteger clientIdSeed;

	private static final Lock authLock = new ReentrantLock();

	public BftsmartClientAuthencationService(BftsmartNodeServer nodeServer) {
		this.nodeServer = nodeServer;
		clientIdSeed = new AtomicInteger(0);
	}

	@Override
	public BftsmartClientIncomingSettings authencateIncoming(ClientIdentification authId) {
		if (verify(authId)) {
			BftsmartTopology topology = nodeServer.getTopology();
			if (topology == null) {
				throw new IllegalStateException("Topology still not created !!!");
			}

			BftsmartClientIncomingConfig clientIncomingSettings = new BftsmartClientIncomingConfig();
			clientIncomingSettings.setTopology(BinarySerializeUtils.serialize(topology));

			clientIncomingSettings.setTomConfig(BinarySerializeUtils.serialize(nodeServer.getTomConfig()));

			clientIncomingSettings.setConsensusSettings(nodeServer.getConsensusSetting());

			clientIncomingSettings.setPubKey(authId.getPubKey());
			// compute gateway id
			authLock.lock();
			try {
				int clientCount = clientIdSeed.getAndIncrement();
				if (clientCount > MAX_CLIENT_COUNT) {
					throw new IllegalStateException(
							String.format("Too many clients income from the node server[%s]! -- MAX_CLIENT_COUNT=%s",
									nodeServer.getId(), MAX_CLIENT_COUNT));
				}
				clientIncomingSettings.setClientId(clientCount * nodeServer.getId());
			} finally {
				authLock.unlock();
			}

			return clientIncomingSettings;
		}

		return null;
	}

	public boolean verify(ClientIdentification authId) {

		SignatureFunction signatureFunction = Crypto.getSignatureFunction(authId.getPubKey().getAlgorithm());

		return signatureFunction.verify(authId.getSignature(), authId.getPubKey(), authId.getIdentityInfo());
	}
}
