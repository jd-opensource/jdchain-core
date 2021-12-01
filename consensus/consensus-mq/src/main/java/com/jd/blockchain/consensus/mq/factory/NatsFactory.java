/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: NatsFactory
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/5 下午10:15
 * Description:
 */
package com.jd.blockchain.consensus.mq.factory;

import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.consumer.NatsConsumer;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.producer.NatsProducer;

/**
 *
 * @author shaozhuguang
 * @create 2018/11/5
 * @since 1.0.0
 */

public class NatsFactory {

    public static MsgQueueProducer newProducer(int clientId, String server, String topic, boolean durable) throws Exception {
        return new NatsProducer(clientId, server, topic, durable);
    }

    public static MsgQueueConsumer newConsumer(int clientId, String server, String topic, boolean durable) throws Exception {
        return new NatsConsumer(clientId, server, topic, durable);
    }
}