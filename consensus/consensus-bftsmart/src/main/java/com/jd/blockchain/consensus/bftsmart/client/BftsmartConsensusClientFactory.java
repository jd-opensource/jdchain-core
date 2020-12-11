package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.CredentialInfo;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientAuthCredit;
import com.jd.blockchain.consensus.bftsmart.BftsmartCredentialInfo;
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

public class BftsmartConsensusClientFactory implements ClientFactory {

	public BftsmartConsensusClientFactory() {

	}

	@Override
	public BftsmartClientAuthCredit buildAuthId(CredentialInfo credentialInfo, AsymmetricKeypair clientKeyPair) {
		if (credentialInfo == null) {
			throw new IllegalArgumentException("No credential info!");
		}
		if (!(credentialInfo instanceof BftsmartCredentialInfo)) {
			throw new IllegalArgumentException(
					"Illegal credential info type! Requrie [" + BftsmartCredentialInfo.class.getName()
							+ "] but it is [" + credentialInfo.getClass().getName() + "]!");
		}

		PubKey pubKey = clientKeyPair.getPubKey();
		PrivKey privKey = clientKeyPair.getPrivKey();

		SignatureFunction signatureFunction = Crypto.getSignatureFunction(pubKey.getAlgorithm());
		
		byte[] credentialBytes = BinaryProtocol.encode(credentialInfo, BftsmartCredentialInfo.class);
		SignatureDigest signatureDigest = signatureFunction.sign(privKey, credentialBytes);

		BftsmartClientAuthCredit bftsmartClientIdentification = new BftsmartClientAuthCredit();
		bftsmartClientIdentification.setCredentialInfo((BftsmartCredentialInfo)credentialInfo);
		bftsmartClientIdentification.setPubKey(pubKey);
		bftsmartClientIdentification.setSignatureDigest(signatureDigest);

		return bftsmartClientIdentification;
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
