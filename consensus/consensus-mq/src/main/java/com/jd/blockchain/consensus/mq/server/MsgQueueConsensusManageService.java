/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.server.MsgQueueConsensusManageService
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午1:46
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.ca.CaType;
import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.ConsensusSecurityException;
import com.jd.blockchain.consensus.mq.client.MQCredentialInfo;
import com.jd.blockchain.consensus.mq.config.MsgQueueClientIncomingConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureFunction;
import utils.StringUtils;

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
		return authencateIncoming(authId, null);
	}

	@Override
	public MsgQueueClientIncomingSettings authencateIncoming(ClientCredential authId, X509Certificate rootCa)
			throws ConsensusSecurityException {
		boolean isLegal = isLegal(authId);
		if(null != rootCa) {
			if(StringUtils.isEmpty(authId.getCertificate())) {
				throw new ConsensusSecurityException("Client certificate is empty!");
			}
			X509Certificate clientCa = X509Utils.resolveCertificate(authId.getCertificate());
			X509Utils.checkValidity(clientCa);
			X509Utils.checkCaTypesAny(clientCa, CaType.PEER, CaType.GW);
			X509Utils.verify(clientCa, rootCa.getPublicKey());
		}
		if (isLegal) {
			MsgQueueClientIncomingSettings mqcis = new MsgQueueClientIncomingConfig().setPubKey(authId.getPubKey())
					.setClientId(clientId(null)).setConsensusSettings(this.consensusSettings)
					.setSessionCredential(authId.getSessionCredential());
			return mqcis;
		}
		return null;
	}

	private int clientId(byte[] identityInfo) {
		// todo

		return 0;
	}

	public boolean isLegal(ClientCredential authId) {
		PubKey pubKey = authId.getPubKey();
		byte[] identityInfo = BinaryProtocol.encode(authId.getSessionCredential(), MQCredentialInfo.class);
		SignatureFunction signatureFunction = Crypto.getSignatureFunction(pubKey.getAlgorithm());
		return signatureFunction.verify(authId.getSignature(), pubKey, identityInfo);
	}
}