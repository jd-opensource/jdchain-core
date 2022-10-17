package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.service.ConsensusMessageContext;

public class MQConsensusMessageContext implements ConsensusMessageContext {

  private String realmName;
  private String batchId;
  private Long timestamp;

  private MQConsensusMessageContext(String realmName) {
    this.realmName = realmName;
  }

  public static MQConsensusMessageContext createInstance(String realmName) {
    return new MQConsensusMessageContext(realmName);
  }

  @Override
  public String getBatchId() {
    return this.batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  @Override
  public String getRealmName() {
    return this.realmName;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }
}
