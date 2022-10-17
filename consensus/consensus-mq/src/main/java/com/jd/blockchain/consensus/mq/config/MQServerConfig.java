/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName:
 * com.jd.blockchain.mq.config.MsgQueueServerConfig Author: shaozhuguang Department: 区块链研发部 Date:
 * 2018/12/12 上午11:32 Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.mq.settings.MQBlockSettings;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MQNodeSettings;
import com.jd.blockchain.consensus.mq.settings.MQServerSettings;

/**
 * peer节点配置
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */
public class MQServerConfig implements MQServerSettings {

  private MQBlockSettings blockSettings;

  private MQConsensusSettings consensusSettings;

  private MQNodeSettings nodeSettings;

  private String realmName;

  public MQServerConfig setNodeSettings(MQNodeSettings nodeSettings) {
    this.nodeSettings = nodeSettings;
    return this;
  }

  @Override
  public String getRealmName() {
    return this.realmName;
  }

  public MQServerConfig setRealmName(String realmName) {
    this.realmName = realmName;
    return this;
  }

  @Override
  public MQNodeSettings getReplicaSettings() {
    return nodeSettings;
  }

  @Override
  public MQBlockSettings getBlockSettings() {
    return blockSettings;
  }

  public MQServerConfig setBlockSettings(MQBlockSettings blockSettings) {
    this.blockSettings = blockSettings;
    return this;
  }

  @Override
  public MQConsensusSettings getConsensusSettings() {
    return consensusSettings;
  }

  public MQServerConfig setConsensusSettings(MQConsensusSettings consensusSettings) {
    this.consensusSettings = consensusSettings;
    return setBlockSettings(consensusSettings.getBlockSettings());
  }

  @Override
  public MQNodeSettings getMQNodeSettings() {
    return nodeSettings;
  }
}
