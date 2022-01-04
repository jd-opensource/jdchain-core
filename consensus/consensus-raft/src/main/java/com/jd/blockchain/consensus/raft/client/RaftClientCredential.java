package com.jd.blockchain.consensus.raft.client;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.raft.RaftConsensusProvider;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;

public class RaftClientCredential implements ClientCredential {

    static {
        DataContractRegistry.register(ClientCredential.class);
    }

    private SessionCredential identityInfo;

    private PubKey pubKey;

    private SignatureDigest signature;

    private String certificate;

    public RaftClientCredential() {
    }

    public RaftClientCredential(ClientCredential clientIdentification) {
        identityInfo = clientIdentification.getSessionCredential();
        pubKey = clientIdentification.getPubKey();
        signature = clientIdentification.getSignature();
    }

    public SessionCredential getIdentityInfo() {
        return identityInfo;
    }

    public void setIdentityInfo(SessionCredential identityInfo) {
        this.identityInfo = identityInfo;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }

    public void setSignature(SignatureDigest signature) {
        this.signature = signature;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    @Override
    public SessionCredential getSessionCredential() {
        return this.identityInfo;
    }

    @Override
    public PubKey getPubKey() {
        return this.pubKey;
    }

    @Override
    public SignatureDigest getSignature() {
        return this.signature;
    }

    @Override
    public String getProviderName() {
        return RaftConsensusProvider.PROVIDER_NAME;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }


}