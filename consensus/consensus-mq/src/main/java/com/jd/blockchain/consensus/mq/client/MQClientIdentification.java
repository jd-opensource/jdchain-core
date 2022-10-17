/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.client.MsgQueueClientIdentification
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午2:04
 * Description:
 */
package com.jd.blockchain.consensus.mq.client;

import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MQClientIdentification implements ClientCredential {

    private SessionCredential identityInfo;

    private PubKey pubKey;

    private SignatureDigest signature;

    private String certificate;

    public MQClientIdentification() {
    }

    public MQClientIdentification(ClientCredential clientIdentification) {
        identityInfo = clientIdentification.getSessionCredential();
        pubKey = clientIdentification.getPubKey();
        signature = clientIdentification.getSignature();
    }

    public MQClientIdentification setIdentityInfo(SessionCredential identityInfo) {
        this.identityInfo = identityInfo;
        return this;
    }

    public MQClientIdentification setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
        return this;
    }

    public MQClientIdentification setSignature(SignatureDigest signature) {
        this.signature = signature;
        return this;
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
        return MsgQueueConsensusProvider.NAME;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }

    public MQClientIdentification setCertificate(String certificate) {
        this.certificate = certificate;
        return this;
    }
}