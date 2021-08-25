package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientAuthCredit;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.client.ClientFactory;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;
import com.jd.blockchain.crypto.SignatureFunction;

import java.security.cert.X509Certificate;

public class BftsmartConsensusClientFactory implements ClientFactory {

	public BftsmartConsensusClientFactory() {

	}

	@Override
	public BftsmartClientAuthCredit buildCredential(SessionCredential sessionCredential, AsymmetricKeypair clientKeyPair) {
		return buildCredential(sessionCredential, clientKeyPair, null);
	}

	@Override
	public BftsmartClientAuthCredit buildCredential(SessionCredential sessionCredential, AsymmetricKeypair clientKeyPair, X509Certificate gatewayCertificate) {
		if (sessionCredential == null) {
			sessionCredential = BftsmartSessionCredentialConfig.createEmptyCredential();
		}else
		if (!(sessionCredential instanceof BftsmartSessionCredential)) {
			throw new IllegalArgumentException(
					"Illegal credential info type! Requrie [" + BftsmartSessionCredential.class.getName()
							+ "] but it is [" + sessionCredential.getClass().getName() + "]!");
		}

		PubKey pubKey = clientKeyPair.getPubKey();
		PrivKey privKey = clientKeyPair.getPrivKey();

		SignatureFunction signatureFunction = Crypto.getSignatureFunction(pubKey.getAlgorithm());
		
		byte[] credentialBytes = BinaryProtocol.encode(sessionCredential, BftsmartSessionCredential.class);
		SignatureDigest signatureDigest = signatureFunction.sign(privKey, credentialBytes);

		BftsmartClientAuthCredit bftsmartClientAuthCredential = new BftsmartClientAuthCredit();
		bftsmartClientAuthCredential.setSessionCredential((BftsmartSessionCredential)sessionCredential);
		bftsmartClientAuthCredential.setPubKey(pubKey);
		bftsmartClientAuthCredential.setSignatureDigest(signatureDigest);
		bftsmartClientAuthCredential.setCertificate(null != gatewayCertificate ? X509Utils.toPEMString(gatewayCertificate) : null);

		return bftsmartClientAuthCredential;
	}

	@Override
	public ClientSettings buildClientSettings(ClientIncomingSettings incomingSettings) {

		BftsmartClientIncomingSettings clientIncomingSettings = (BftsmartClientIncomingSettings) incomingSettings;

		BftsmartClientSettings clientSettings = new BftsmartClientConfig(clientIncomingSettings);

		return clientSettings;
	}

	@Override
	public ConsensusClient setupClient(ClientSettings settings) {
		return new BftsmartConsensusClient((BftsmartClientSettings) settings);
	}

}
