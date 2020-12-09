package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.ClientIncomingSettings;
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
	public BftsmartClientIdentification buildAuthId(AsymmetricKeypair clientKeyPair) {

		PubKey pubKey = clientKeyPair.getPubKey();
		PrivKey privKey = clientKeyPair.getPrivKey();

		SignatureFunction signatureFunction =Crypto.getSignatureFunction(pubKey.getAlgorithm());
		SignatureDigest signatureDigest = signatureFunction.sign(privKey, pubKey.toBytes());

		BftsmartClientIdentification bftsmartClientIdentification = new BftsmartClientIdentification();
		bftsmartClientIdentification.setIdentityInfo(pubKey.toBytes());
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
		return new BftsmartConsensusClient(settings);
	}

}
