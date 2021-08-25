package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;

public class BftsmartClientAuthCredit implements ClientCredential {

	private BftsmartSessionCredential sessionCredential;
	private PubKey pubKey;
	private SignatureDigest signatureDigest;
	private String certificate;

	public BftsmartClientAuthCredit() {
	}

	public BftsmartClientAuthCredit(ClientCredential clientCredential) {
		if (clientCredential.getSessionCredential() == null) {
			throw new IllegalArgumentException("Client credential info is null!");
		}
		if (!(clientCredential.getSessionCredential() instanceof BftsmartSessionCredential)) {
			throw new IllegalArgumentException(
					"Require the client credential info of type [" + BftsmartSessionCredential.class.getName()
							+ "], but it is [" + clientCredential.getSessionCredential().getClass().getName() + "]!");
		}
		this.sessionCredential = (BftsmartSessionCredential) clientCredential.getSessionCredential();
		this.pubKey = clientCredential.getPubKey();
		this.signatureDigest = clientCredential.getSignature();
	}


	@Override
	public BftsmartSessionCredential getSessionCredential() {
		return sessionCredential;
	}

	public void setSessionCredential(BftsmartSessionCredential sessionCredential) {
		this.sessionCredential = sessionCredential;
	}

	@Override
	public PubKey getPubKey() {
		return pubKey;
	}

	public void setPubKey(PubKey pubKey) {
		this.pubKey = pubKey;
	}

	@Override
	public SignatureDigest getSignature() {
		return signatureDigest;
	}

	@Override
	public String getProviderName() {
		return BftsmartConsensusProvider.NAME;
	}

	@Override
	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public void setSignatureDigest(SignatureDigest signatureDigest) {
		this.signatureDigest = signatureDigest;
	}

}
