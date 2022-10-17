package com.jd.blockchain.consensus.mq.event;

import com.jd.blockchain.consensus.mq.event.binaryproto.ProposeEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.TxEvent;
import com.jd.blockchain.crypto.Crypto;

import java.util.Arrays;
import java.util.List;

public class ProposeMessage implements ProposeEvent {

  private int proposer;
  private long latestHeight;
  private byte[] latestHash;
  private long timestamp;
  private TxEvent[] txs;

  public ProposeMessage(int proposer, long timestamp, long latestHeight, byte[] latestHash) {
    this.proposer = proposer;
    this.latestHeight = latestHeight;
    this.latestHash = latestHash;
    this.timestamp = timestamp;
  }

  public ProposeMessage(
      int proposer, long timestamp, long latestHeight, byte[] latestHash, List<TxEvent> txs) {
    this.proposer = proposer;
    this.latestHeight = latestHeight;
    this.latestHash = latestHash;
    this.timestamp = timestamp;
    this.txs = null != txs ? txs.toArray(new TxEvent[txs.size()]) : null;
  }

  public ProposeMessage(ProposeEvent propose) {
    this.proposer = propose.getProposer();
    this.latestHeight = propose.getLatestHeight();
    this.latestHash = propose.getLatestHash();
    this.timestamp = propose.getTimestamp();
    this.txs = propose.getTxs();
  }

  @Override
  public int getProposer() {
    return proposer;
  }

  public void setProposer(int proposer) {
    this.proposer = proposer;
  }

  @Override
  public long getLatestHeight() {
    return latestHeight;
  }

  public void setLatestHeight(long latestHeight) {
    this.latestHeight = latestHeight;
  }

  @Override
  public byte[] getLatestHash() {
    return latestHash;
  }

  public void setLatestHash(byte[] latestHash) {
    this.latestHash = latestHash;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public TxEvent[] getTxs() {
    return txs;
  }

  public void setTxs(List<TxEvent> txs) {
    this.txs = null != txs ? txs.toArray(new TxEvent[txs.size()]) : null;
  }

  @Override
  public String toString() {
    return "ProposeMessage{"
        + "proposer="
        + proposer
        + ", latestHeight="
        + latestHeight
        + ", latestHash="
        + Crypto.resolveAsHashDigest(latestHash)
        + ", timestamp="
        + timestamp
        + ", txs="
        + (null != txs ? Arrays.toString(Arrays.stream(txs).map(TxEvent::getKey).toArray()) : "[]")
        + '}';
  }
}
