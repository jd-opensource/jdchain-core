/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName:
 * com.jd.blockchain.mq.config.MsgQueueClientIncomingConfig Author: shaozhuguang Department: 区块链研发部
 * Date: 2018/12/12 上午11:50 Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider;
import com.jd.blockchain.consensus.mq.settings.MQClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.crypto.PubKey;

/**
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */
public class MQClientIncomingConfig implements MQClientIncomingSettings {

    private int clientId;

    private PubKey pubKey;

    private MQConsensusSettings consensusSettings;

    private SessionCredential sessionCredential;

    public MQClientIncomingConfig setConsensusSettings(
            MQConsensusSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
        return this;
    }

    public MQClientIncomingConfig setSessionCredential(SessionCredential sessionCredential) {
        this.sessionCredential = sessionCredential;
        return this;
    }

    @Override
    public int getClientId() {
        return this.clientId;
    }

    public MQClientIncomingConfig setClientId(int clientId) {
        this.clientId = clientId;
        return this;
    }

    @Override
    public String getProviderName() {
        return MsgQueueConsensusProvider.NAME;
    }

    @Override
    public MQConsensusSettings getViewSettings() {
        return this.consensusSettings;
    }

    @Override
    public PubKey getPubKey() {
        return pubKey;
    }

    public MQClientIncomingConfig setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
        return this;
    }

    @Override
    public SessionCredential getCredential() {
        return sessionCredential;
    }
}
