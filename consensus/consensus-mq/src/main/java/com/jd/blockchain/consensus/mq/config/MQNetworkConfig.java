/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName:
 * com.jd.blockchain.consensus.mq.config.MsgQueueNetworkConfig Author: shaozhuguang Department:
 * 区块链研发部 Date: 2018/12/12 下午4:55 Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.mq.settings.MQNetworkSettings;

/**
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */
public class MQNetworkConfig implements MQNetworkSettings {

  private String server;
  private String txTopic;
  private String txResultTopic;
  private String blockTopic;
  private String msgTopic;
  private String msgResultTopic;

  @Override
  public String getServer() {
    return server;
  }

  public MQNetworkConfig setServer(String server) {
    this.server = server;
    return this;
  }

  @Override
  public String getTxTopic() {
    return txTopic;
  }

  public MQNetworkConfig setTxTopic(String txTopic) {
    this.txTopic = txTopic;
    return this;
  }

  @Override
  public String getTxResultTopic() {
    return txResultTopic;
  }

  public MQNetworkConfig setTxResultTopic(String txResultTopic) {
    this.txResultTopic = txResultTopic;
    return this;
  }

  @Override
  public String getMsgTopic() {
    return msgTopic;
  }

  public MQNetworkConfig setMsgTopic(String msgTopic) {
    this.msgTopic = msgTopic;
    return this;
  }

  @Override
  public String getBlockTopic() {
    return blockTopic;
  }

  public MQNetworkConfig setBlockTopic(String blockTopic) {
    this.blockTopic = blockTopic;
    return this;
  }

  @Override
  public String getMsgResultTopic() {
    return msgResultTopic;
  }

  public MQNetworkConfig setMsgResultTopic(String msgResultTopic) {
    this.msgResultTopic = msgResultTopic;
    return this;
  }
}
