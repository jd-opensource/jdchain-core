/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.server.MsgQueueConsensusManageService
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午1:46
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import java.util.Arrays;

import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.ConsensusSecurityException;
import com.jd.blockchain.consensus.mq.client.MQCredentialInfo;
import com.jd.blockchain.consensus.mq.config.MsgQueueClientIncomingConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureFunction;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MsgQueueConsensusManageService implements ClientAuthencationService {

	private MsgQueueConsensusSettings consensusSettings;

	public MsgQueueConsensusManageService setConsensusSettings(MsgQueueConsensusSettings consensusSettings) {
		this.consensusSettings = consensusSettings;
		return this;
	}

	@Override
	public MsgQueueClientIncomingSettings authencateIncoming(ClientCredential authId)
			throws ConsensusSecurityException {
		boolean isLegal = isLegal(authId);
		if (isLegal) {
			MsgQueueClientIncomingSettings mqcis = new MsgQueueClientIncomingConfig().setPubKey(authId.getPubKey())
					.setClientId(clientId(null)).setConsensusSettings(this.consensusSettings);
			return mqcis;
		}
		return null;
	}

	private int clientId(byte[] identityInfo) {
		// todo

		return 0;
	}

	public boolean isLegal(ClientCredential authId) {
		if (!(authId.getSessionCredential() instanceof MQCredentialInfo)) {
			return false;
		}
		boolean isLegal = false;
		PubKey pubKey = authId.getPubKey();
		byte[] identityInfo = BinaryProtocol.encode(authId.getSessionCredential(), MQCredentialInfo.class);
		byte[] address = pubKey.toBytes(); // 使用公钥地址作为认证信息
		if (Arrays.equals(address, identityInfo)) {
			SignatureFunction signatureFunction = Crypto.getSignatureFunction(pubKey.getAlgorithm());
			isLegal = signatureFunction.verify(authId.getSignature(), pubKey, identityInfo);
		}
		return isLegal;
	}
}