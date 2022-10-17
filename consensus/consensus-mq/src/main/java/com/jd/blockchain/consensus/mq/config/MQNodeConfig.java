/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName:
 * com.jd.blockchain.mq.config.MsgQueueNodeConfig Author: shaozhuguang Department: 区块链研发部 Date:
 * 2018/12/12 上午11:33 Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.mq.settings.MQNodeSettings;
import com.jd.blockchain.crypto.PubKey;

/**
 * peer节点IP
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */
public class MQNodeConfig implements MQNodeSettings {

  private int id = -1;
  private String address;
  private PubKey pubKey;
  private String host;

  @Override
  public String getAddress() {
    return this.address;
  }

  public MQNodeConfig setAddress(String address) {
    this.address = address;
    return this;
  }

  @Override
  public PubKey getPubKey() {
    return this.pubKey;
  }

  public MQNodeConfig setPubKey(PubKey pubKey) {
    this.pubKey = pubKey;
    return this;
  }

  @Override
  public int getId() {
    return id;
  }

  public MQNodeConfig setId(int id) {
    this.id = id;
    return this;
  }

  @Override
  public String getHost() {
    return host;
  }

  public MQNodeConfig setHost(String host) {
    this.host = host;
    return this;
  }
}
