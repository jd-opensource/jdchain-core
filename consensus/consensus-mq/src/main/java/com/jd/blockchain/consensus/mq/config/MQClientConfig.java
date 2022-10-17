/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName:
 * com.jd.blockchain.consensus.mq.config.MsgQueueClientConfig Author: shaozhuguang Department:
 * 区块链研发部 Date: 2018/12/12 下午2:23 Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.mq.settings.MQClientSettings;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MQNetworkSettings;
import com.jd.blockchain.crypto.PubKey;

/**
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */
public class MQClientConfig implements MQClientSettings {

  private int id;

  private PubKey pubKey;

  private MQConsensusSettings consensusSettings;

  public MQClientConfig setId(int id) {
    this.id = id;
    return this;
  }

  public MQClientConfig setPubKey(PubKey pubKey) {
    this.pubKey = pubKey;
    return this;
  }

  public MQClientConfig setConsensusSettings(MQConsensusSettings consensusSettings) {
    this.consensusSettings = consensusSettings;
    return this;
  }

  @Override
  public int getClientId() {
    return this.id;
  }

  @Override
  public PubKey getClientPubKey() {
    return this.pubKey;
  }

  @Override
  public MQConsensusSettings getViewSettings() {
    return this.consensusSettings;
  }

  @Override
  public MQNetworkSettings getMsgQueueNetworkSettings() {
    return this.consensusSettings.getNetworkSettings();
  }

  @Override
  public SessionCredential getSessionCredential() {
    // TODO Auto-generated method stub
    throw new IllegalStateException("Not implemented!");
  }
}
