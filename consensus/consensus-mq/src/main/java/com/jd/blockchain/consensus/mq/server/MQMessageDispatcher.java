package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.MQEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.ProposeEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.TxEvent;

import java.io.Closeable;
import java.util.List;

public interface MQMessageDispatcher extends Runnable, Closeable {

  /**
   * 连接
   *
   * @throws Exception
   */
  void connect() throws Exception;

  /** 定时提议 */
  void onProposeTime();

  /**
   * 新交易
   *
   * @param tx
   */
  void onTx(MQEvent tx) throws Exception;

  /** 提议区块，仅发出提议消息，不包含交易列表 */
  void propose();

  /**
   * 提议区块，包含交易列表
   *
   * @param txs
   */
  void propose(List<TxEvent> txs);

  /**
   * 生成新区块
   *
   * @param propose
   */
  void onPropose(ProposeEvent propose) throws Exception;

  /**
   * 监听到非共识消息
   *
   * @param exMsg
   */
  void onMessage(ExtendEvent exMsg) throws Exception;
}
