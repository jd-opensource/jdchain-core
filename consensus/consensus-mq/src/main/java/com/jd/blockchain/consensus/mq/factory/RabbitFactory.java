/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.sdk.nats.RabbitFactory
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/5 下午10:15
 * Description:
 */
package com.jd.blockchain.consensus.mq.factory;

import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.consumer.RabbitConsumer;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.producer.RabbitProducer;
import com.rabbitmq.client.ConnectionFactory;
import utils.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author shaozhuguang
 * @create 2018/11/5
 * @since 1.0.0
 */

public class RabbitFactory {

    public static MsgQueueProducer newProducer(int clientId, String server, String topic, boolean durable) throws Exception {
        return new RabbitProducer(clientId, server, topic, durable);
    }

    public static MsgQueueConsumer newConsumer(int clientId, String server, String topic, boolean durable) throws Exception {
        return new RabbitConsumer(clientId, server, topic, durable);
    }

    public static ConnectionFactory initConnectionFactory(String server) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(server);
        return factory;
    }
}