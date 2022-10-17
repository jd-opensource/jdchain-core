/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName: MsgQueueProducer Author:
 * shaozhuguang Department: 区块链研发部 Date: 2018/11/5 下午10:37 Description:
 */
package com.jd.blockchain.consensus.mq.producer;

import java.io.Closeable;

/**
 * @author shaozhuguang
 * @create 2018/11/5
 * @since 1.0.0
 */
public interface MQProducer extends Closeable {

  void connect() throws Exception;

  void publish(byte[] message) throws Exception;
}
