package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;

public class BftsmartClientAuthCredit implements ClientCredential {

	private BftsmartCredentialInfo credentialInfo;
	private PubKey pubKey;
	private SignatureDigest signatureDigest;

	public BftsmartClientAuthCredit() {
	}

	public BftsmartClientAuthCredit(ClientCredential clientCredential) {
		if (clientCredential.getCredentialInfo() == null) {
			throw new IllegalArgumentException("Client credential info is null!");
		}
		if (!(clientCredential.getCredentialInfo() instanceof BftsmartCredentialInfo)) {
			throw new IllegalArgumentException(
					"Require the client credential info of type [" + BftsmartCredentialInfo.class.getName()
							+ "], but it is [" + clientCredential.getCredentialInfo().getClass().getName() + "]!");
		}
		this.credentialInfo = (BftsmartCredentialInfo) clientCredential.getCredentialInfo();
		this.pubKey = clientCredential.getPubKey();
		this.signatureDigest = clientCredential.getSignature();
	}


	@Override
	public BftsmartCredentialInfo getCredentialInfo() {
		return credentialInfo;
	}

	public void setCredentialInfo(BftsmartCredentialInfo credentialInfo) {
		this.credentialInfo = credentialInfo;
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

	public void setSignatureDigest(SignatureDigest signatureDigest) {
		this.signatureDigest = signatureDigest;
	}

}
