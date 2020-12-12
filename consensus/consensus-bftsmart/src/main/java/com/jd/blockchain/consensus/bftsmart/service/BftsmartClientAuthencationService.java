package com.jd.blockchain.consensus.bftsmart.service;

import java.util.concurrent.atomic.AtomicInteger;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingConfig;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartSessionCredentialConfig;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.utils.serialize.binary.BinarySerializeUtils;

public class BftsmartClientAuthencationService implements ClientAuthencationService {

	public static final int MAX_CLIENT_COUNT = 200000;

	public static final int POOL_SIZE_PEER_CLIENT = 50;

	/**
	 * 全局的客户端ID最小值；
	 * <p>
	 * 
	 * 在最大的共识节点ID 的基础上多预留 1 倍的 ID 分配空间，用于共识节点之间互联的共识客户端 ID；
	 */
	private static final int GLOBAL_MIN_CLIENT_ID = BftsmartNodeServer.MAX_SERVER_ID * 2;

	/**
	 * 当前节点分配空间下的客户端ID起始值，也是最小值；
	 */
	private final int LOCAL_CLIENT_ID_BASE;

	private BftsmartNodeServer nodeServer;

	private AtomicInteger clientIdSeed;

	public BftsmartClientAuthencationService(BftsmartNodeServer nodeServer) {
		this.nodeServer = nodeServer;
		clientIdSeed = new AtomicInteger(0);
		LOCAL_CLIENT_ID_BASE = computeLocalMinClientId(nodeServer.getId());
	}

	@Override
	public BftsmartClientIncomingSettings authencateIncoming(ClientCredential clientCredential) {
		if (!verify(clientCredential)) {
			return null;
		}
		BftsmartTopology topology = nodeServer.getTopology();
		if (topology == null) {
			throw new IllegalStateException("Topology of node[" + nodeServer.getId() + "] still not created !!!");
		}

		BftsmartClientIncomingConfig clientIncomingSettings = new BftsmartClientIncomingConfig();
		clientIncomingSettings.setTopology(BinarySerializeUtils.serialize(topology));
		clientIncomingSettings.setTomConfig(BinarySerializeUtils.serialize(nodeServer.getTomConfig()));
		clientIncomingSettings.setViewSettings(nodeServer.getConsensusSetting());
		clientIncomingSettings.setPubKey(clientCredential.getPubKey());

		BftsmartSessionCredential sessionCredential = (BftsmartSessionCredential) clientCredential
				.getSessionCredential();

		// 如果历史会话凭证的客户端ID是小于全局的最小客户端ID，则是无效的客户端ID，对其重新分配；
		// 注：忽略历史会话凭证的客户端ID不属于当前节点的分配空间的情形，此种情形是由于该客户端是从其它共识节点重定向过来的，
		// 应该继续维持该客户端的 ID 复用；
		if (sessionCredential.getClientId() < GLOBAL_MIN_CLIENT_ID || sessionCredential.getClientIdRange() < 1) {
			// 重新分配
			int idRange = POOL_SIZE_PEER_CLIENT;
			int clientId = allocateClientId(idRange);
			sessionCredential = new BftsmartSessionCredentialConfig(clientId, idRange, System.currentTimeMillis());
		}
		clientIncomingSettings.setSessionCredential(sessionCredential);

		return clientIncomingSettings;
	}

	private int allocateClientId(int clientIdRange) {
		int clientSequence;
		do {
			clientSequence = clientIdSeed.get();
		} while (clientSequence < MAX_CLIENT_COUNT && !clientIdSeed.compareAndSet(clientSequence, clientSequence + 1));

		if (clientSequence >= MAX_CLIENT_COUNT) {
			throw new IllegalStateException(
					String.format("Too many clients income from the node server[%s]! -- MAX_CLIENT_COUNT=%s",
							nodeServer.getId(), MAX_CLIENT_COUNT));
		}

		return LOCAL_CLIENT_ID_BASE + clientSequence * clientIdRange;
	}

	private boolean verify(ClientCredential credential) {
		byte[] credentialBytes = BinaryProtocol.encode(credential.getSessionCredential(),
				BftsmartSessionCredential.class);
		SignatureFunction signatureFunction = Crypto.getSignatureFunction(credential.getPubKey().getAlgorithm());

		return signatureFunction.verify(credential.getSignature(), credential.getPubKey(), credentialBytes);
	}

	public static int allocateClientIdForPeer(int nodeServerId) {
		assert nodeServerId >= 0 && nodeServerId < BftsmartNodeServer.MAX_SERVER_ID;
		return BftsmartNodeServer.MAX_SERVER_ID + nodeServerId;
	}

	public static int allocateClientId(int clientSeqence, int nodeServerId, int clientIdRange) {
		assert clientSeqence >= 0 && nodeServerId >= 0 && nodeServerId < BftsmartNodeServer.MAX_SERVER_ID;
		return computeLocalMinClientId(nodeServerId) + clientSeqence * clientIdRange;
	}

	public static int computeLocalMinClientId(int nodeServerId) {
		return GLOBAL_MIN_CLIENT_ID + nodeServerId * MAX_CLIENT_COUNT * POOL_SIZE_PEER_CLIENT;
	}
}
