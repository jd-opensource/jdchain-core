package com.jd.blockchain.consensus.bftsmart.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.bftsmart.BftsmartCredentialInfo;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingConfig;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartClientId;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.utils.serialize.binary.BinarySerializeUtils;

public class BftsmartClientAuthencationService implements ClientAuthencationService {

	public static final int MAX_CLIENT_COUNT = 200000;

	public static final int POOL_SIZE_PEER_CLIENT = 50;

	private BftsmartNodeServer nodeServer;

	private AtomicInteger clientIdSeed;

	private static final Lock authLock = new ReentrantLock();

	public BftsmartClientAuthencationService(BftsmartNodeServer nodeServer) {
		this.nodeServer = nodeServer;
		clientIdSeed = new AtomicInteger(0);
	}

	@Override
	public BftsmartClientIncomingSettings authencateIncoming(ClientCredential credential) {
		if (verify(credential)) {
			BftsmartTopology topology = nodeServer.getTopology();
			if (topology == null) {
				throw new IllegalStateException("Topology of node[" + nodeServer.getId() + "] still not created !!!");
			}

			BftsmartClientIncomingConfig clientIncomingSettings = new BftsmartClientIncomingConfig();
			clientIncomingSettings.setTopology(BinarySerializeUtils.serialize(topology));

			clientIncomingSettings.setTomConfig(BinarySerializeUtils.serialize(nodeServer.getTomConfig()));

			clientIncomingSettings.setViewSettings(nodeServer.getConsensusSetting());

			clientIncomingSettings.setPubKey(credential.getPubKey());
			
			
			// compute gateway id
			authLock.lock();
			try {
				int clientCount = clientIdSeed.getAndIncrement();
				if (clientCount >= MAX_CLIENT_COUNT) {
					throw new IllegalStateException(
							String.format("Too many clients income from the node server[%s]! -- MAX_CLIENT_COUNT=%s",
									nodeServer.getId(), MAX_CLIENT_COUNT));
				}

				int clientId = allocateClientId(clientCount, nodeServer.getId(), POOL_SIZE_PEER_CLIENT);
				
				clientIncomingSettings.setCredentialInfo(new BftsmartClientId(clientId, POOL_SIZE_PEER_CLIENT));
			} finally {
				authLock.unlock();
			}

			return clientIncomingSettings;
		}

		return null;
	}

	private boolean verify(ClientCredential credential) {

		byte[] credentialBytes = BinaryProtocol.encode(credential.getCredentialInfo(), BftsmartCredentialInfo.class);
		SignatureFunction signatureFunction = Crypto.getSignatureFunction(credential.getPubKey().getAlgorithm());

		return signatureFunction.verify(credential.getSignature(), credential.getPubKey(),
				credentialBytes);
	}

	public static int allocateClientId(int clientSeqence, int nodeServerId, int clientIdRange) {
		assert clientSeqence >= 0 && nodeServerId >= 0;

		return BftsmartNodeServer.MAX_SERVER_ID + nodeServerId * MAX_CLIENT_COUNT * POOL_SIZE_PEER_CLIENT
				+ clientSeqence * clientIdRange;
	}
}
