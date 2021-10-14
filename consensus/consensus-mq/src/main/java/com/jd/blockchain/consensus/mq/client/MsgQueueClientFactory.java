/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.client.MsgQueueClientFactory
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 上午11:23
 * Description:
 */
package com.jd.blockchain.consensus.mq.client;

import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.client.ClientFactory;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.mq.config.MsgQueueClientConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;
import com.jd.blockchain.crypto.SignatureFunction;

import java.security.cert.X509Certificate;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MsgQueueClientFactory implements ClientFactory {

    @Override
    public MsgQueueClientIdentification buildCredential(SessionCredential sessionCredential, AsymmetricKeypair clientKeyPair) {
        return buildCredential(sessionCredential, clientKeyPair, null);
    }

    @Override
    public MsgQueueClientIdentification buildCredential(SessionCredential credentialInfo, AsymmetricKeypair clientKeyPair, X509Certificate gatewayCertificate) {
    	MQCredentialInfo mqCredentialInfo = (MQCredentialInfo) credentialInfo;
    	if(null == mqCredentialInfo) {
            mqCredentialInfo = MQCredentialConfig.createEmptyCredential();
        }
        byte[] credentialBytes = BinaryProtocol.encode(mqCredentialInfo, MQCredentialInfo.class);

        PubKey pubKey = clientKeyPair.getPubKey();
//        byte[] address = pubKey.toBytes(); // 使用公钥地址作为认证信息

        SignatureFunction signatureFunction = Crypto.getSignatureFunction(pubKey.getAlgorithm());
        SignatureDigest signatureDigest = signatureFunction.sign(clientKeyPair.getPrivKey(), credentialBytes);

        MsgQueueClientIdentification mqci = new MsgQueueClientIdentification()
                .setPubKey(clientKeyPair.getPubKey())
                .setIdentityInfo(mqCredentialInfo)
                .setSignature(signatureDigest)
                .setCertificate(gatewayCertificate != null ? CertificateUtils.toPEMString(gatewayCertificate) : null);
                ;
        return mqci;
    }

    @Override
    public MsgQueueClientSettings buildClientSettings(ClientIncomingSettings incomingSettings) {

        MsgQueueClientIncomingSettings mqcic = (MsgQueueClientIncomingSettings)incomingSettings;
        if (mqcic != null) {
            return buildClientSettings(mqcic.getClientId(), mqcic.getPubKey(), (MsgQueueConsensusSettings)(mqcic.getViewSettings()));
        }
        throw new IllegalArgumentException("ClientIncomingSettings data isn't supported! Accept MsgQueueClientIncomingSettings only!");
    }

    private MsgQueueClientSettings buildClientSettings(int clientId, PubKey pubKey, MsgQueueConsensusSettings mqcs) {

        MsgQueueClientSettings msgQueueClientConfig = new MsgQueueClientConfig()
                .setId(clientId)
                .setPubKey(pubKey)
                .setConsensusSettings(mqcs)
                ;
        return msgQueueClientConfig;
    }

//    @Override
//    public ConsensusManageService createManageServiceClient(String[] serviceNodes) {
//        // todo serviceNodes // IP：port
//        return null;
//    }

    @Override
    public MsgQueueConsensusClient setupClient(ClientSettings settings) {
        if (settings instanceof MsgQueueClientSettings) {
            return setupClient((MsgQueueClientSettings)settings);
        }
        throw new IllegalArgumentException("ClientSettings data isn't supported! Accept MsgQueueClientSettings only!");
    }

    private MsgQueueConsensusClient setupClient(MsgQueueClientSettings settings) {
        MsgQueueConsensusClient mqcc = new MsgQueueConsensusClient()
                .setClientSettings(settings)
                .setMsgQueueNetworkSettings(settings.getMsgQueueNetworkSettings())
                ;
        mqcc.init();
        return mqcc;
    }
}