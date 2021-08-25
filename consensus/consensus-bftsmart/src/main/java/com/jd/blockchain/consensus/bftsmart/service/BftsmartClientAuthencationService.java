package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.ca.CaType;
import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.consensus.ConsensusSecurityException;
import org.bouncycastle.jcajce.provider.asymmetric.X509;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingConfig;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartSessionCredentialConfig;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.SignatureFunction;

import utils.StringUtils;
import utils.io.Storage;
import utils.serialize.binary.BinarySerializeUtils;

import java.security.cert.X509Certificate;

public class BftsmartClientAuthencationService implements ClientAuthencationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BftsmartClientAuthencationService.class);

	public static final int MAX_CLIENT_COUNT = 500000;

	public static final int POOL_SIZE_PEER_CLIENT = 20;

	/**
	 * 保留的系统 ID 范围大小；
	 */
	public static final int RESERVED_ID_RANGE = 1000;

	/**
	 * 全局的客户端ID最小值；
	 * <p>
	 * 
	 * 在最大的共识节点ID 的基础上多预留 1 倍的 ID 分配空间，以及系统ID保留区，用于共识节点之间互联的共识客户端 ID；
	 */
	private static final int GLOBAL_MIN_CLIENT_ID = BftsmartNodeServer.MAX_SERVER_ID * 2 + RESERVED_ID_RANGE;

	/**
	 * 当前节点分配空间下的客户端ID起始值，也是最小值；
	 */
	private final int LOCAL_CLIENT_ID_BASE;

	private final String ID_SEED_STORAGE_KEY;

	private BftsmartNodeServer nodeServer;

	private int clientIdSeed;

	private Storage storage;

	public BftsmartClientAuthencationService(BftsmartNodeServer nodeServer, Storage storage) {
		this.ID_SEED_STORAGE_KEY = "N" + nodeServer.getId() + "-CLIENT-ID-SEED";
		this.LOCAL_CLIENT_ID_BASE = computeLocalMinClientId(nodeServer.getId());

		this.nodeServer = nodeServer;
		this.storage = storage;
		this.clientIdSeed = storage.readInt(ID_SEED_STORAGE_KEY);
	}

	@Override
	public BftsmartClientIncomingSettings authencateIncoming(ClientCredential clientCredential) throws ConsensusSecurityException{
		return authencateIncoming(clientCredential, null);
	}

	@Override
	public BftsmartClientIncomingSettings authencateIncoming(ClientCredential clientCredential, X509Certificate rootCa) throws ConsensusSecurityException {
		if (!verify(clientCredential)) {
			return null;
		}
		if(null != rootCa) {
			// 证书模式下校验接入客户端证书
			if(StringUtils.isEmpty(clientCredential.getCertificate())) {
				throw new ConsensusSecurityException("Client certificate is empty!");
			}
			X509Certificate clientCa = X509Utils.resolveCertificate(clientCredential.getCertificate());
			X509Utils.checkValidity(clientCa);
			X509Utils.checkCaTypesAny(clientCa, CaType.PEER, CaType.GW);
			X509Utils.verify(clientCa, rootCa.getPublicKey());
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
		int clientId = sessionCredential.getClientId();
		int clientIdRange = sessionCredential.getClientIdRange();
		if (clientIdRange < 1 || clientIdRange > POOL_SIZE_PEER_CLIENT) {
			clientIdRange = POOL_SIZE_PEER_CLIENT;
		}
		if (clientId < GLOBAL_MIN_CLIENT_ID) {
			// 重新分配
			clientId = allocateClientId(clientIdRange);
		}
		sessionCredential = new BftsmartSessionCredentialConfig(clientId, clientIdRange, System.currentTimeMillis());
		clientIncomingSettings.setSessionCredential(sessionCredential);

		return clientIncomingSettings;
	}

	private synchronized int allocateClientId(int clientIdRange) {
		if (clientIdSeed >= MAX_CLIENT_COUNT) {
			throw new IllegalStateException(
					String.format("Too many clients income from the node server[%s]! -- MAX_CLIENT_COUNT=%s",
							nodeServer.getId(), MAX_CLIENT_COUNT));
		}
		int clientId = LOCAL_CLIENT_ID_BASE + clientIdSeed * clientIdRange;

		clientIdSeed++;
		LOGGER.debug("Allocated client id[{}] with seed[{}] of node server[{}].", clientId, clientIdSeed,
				nodeServer.getId());
		try {
			storage.writeInt(ID_SEED_STORAGE_KEY, clientIdSeed);
		} catch (Exception e) {
			// 出错不影响后续执行；
			LOGGER.warn("Error occurred while persisting the CLIENT_ID_SEED of node server[" + nodeServer.getId()
					+ "]! --" + e.getMessage(), e);
		}

		return clientId;
	}

	private boolean verify(ClientCredential credential) {
		byte[] credentialBytes = BinaryProtocol.encode(credential.getSessionCredential(),
				BftsmartSessionCredential.class);
		SignatureFunction signatureFunction = Crypto.getSignatureFunction(credential.getPubKey().getAlgorithm());

		return signatureFunction.verify(credential.getSignature(), credential.getPubKey(), credentialBytes);
	}

	/**
	 * 分配用于共识节点之间连接的共识客户端会话 id ；
	 * 
	 * @param nodeServerId
	 * @return
	 */
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

	/**
	 * 计算指定节点 Id 分配空间下的客户端 Id 的范围；
	 * 
	 * <p>
	 * 返回结果为 2 个元素的数组，第一个为客户端 Id 的最小值（包含），第 2 个为客户端 Id 的最大值（不包含）；
	 * 
	 * @param nodeServerId
	 * @return
	 */
	public static int[] computeClientIdRangeFromNode(int nodeServerId) {
		if (nodeServerId < 0 || nodeServerId >= BftsmartNodeServer.MAX_SERVER_ID) {
			throw new IllegalArgumentException(
					"The node server id is out of bound[0 - " + BftsmartNodeServer.MAX_SERVER_ID + "]!");
		}
		int[] range = new int[2];
		range[0] = computeLocalMinClientId(nodeServerId);
		range[1] = computeLocalMinClientId(nodeServerId + 1);
		return range;
	}
}
