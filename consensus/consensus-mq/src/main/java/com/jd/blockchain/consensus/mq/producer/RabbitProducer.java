/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.sdk.nats.RabbitProducer
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/5 下午10:39
 * Description:
 */
package com.jd.blockchain.consensus.mq.producer;

import com.jd.blockchain.consensus.mq.factory.RabbitFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author shaozhuguang
 * @create 2018/11/5
 * @since 1.0.0
 */

public class RabbitProducer implements MsgQueueProducer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitProducer.class);

    // 主要操作时发送JMQ请求
    private Channel channel;

    private Connection connection;

    private String exchangeName;

    private String server;

    private int clientId;

    private boolean durable;

    public RabbitProducer(int clientId, String server, String topic, boolean durable) throws Exception {
        this.clientId = clientId;
        this.exchangeName = topic;
        this.server = server;
        this.durable = durable;
    }

    @Override
    public void connect() throws Exception {
        ConnectionFactory factory = RabbitFactory.initConnectionFactory(server);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(this.exchangeName, "fanout", durable);
        logger.info("[*] RabbitProducer[{}, {}] connect success !!!", this.server, this.exchangeName);
    }

    @Override
    public void publish(byte[] message) throws Exception {
        channel.basicPublish(this.exchangeName, "", MessageProperties.PERSISTENT_TEXT_PLAIN, message);
    }

    @Override
    public void publishString(String message) throws Exception {
        publish(message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void publishStringList(List<String> messages) throws Exception {
        for (String message : messages) {
            publishString(message);
        }
    }

    @Override
    public void publishStringArray(String[] messages) throws Exception {
        for (String message : messages) {
            publishString(message);
        }
    }

    @Override
    public void publishBytesArray(byte[][] message) throws Exception {
        for (byte[] bytes : message) {
            publish(bytes);
        }
    }

    @Override
    public void publishBytesList(List<byte[]> messages) throws Exception {
        for (byte[] message : messages) {
            publish(message);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
            connection.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


}