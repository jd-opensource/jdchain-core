/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.sdk.nats.RabbitConsumer
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/5 下午10:40
 * Description:
 */
package com.jd.blockchain.consensus.mq.consumer;

import com.jd.blockchain.consensus.mq.factory.RabbitFactory;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author shaozhuguang
 * @create 2018/11/5
 * @since 1.0.0
 */

public class RabbitConsumer implements MsgQueueConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConsumer.class);
    private final String exchangeName;
    private final String server;
    private final int clientId;
    private final boolean durable;
    private Connection connection;
    private Channel channel;
    private String queueName;
    private MsgQueueHandler msgQueueHandler;

    public RabbitConsumer(int clientId, String server, String topic, boolean durable) {
        this.clientId = clientId;
        this.server = server;
        this.exchangeName = topic;
        this.durable = durable;
    }

    @Override
    public void connect(MsgQueueHandler msgQueueHandler) throws Exception {
        this.msgQueueHandler = msgQueueHandler;
        ConnectionFactory factory = RabbitFactory.initConnectionFactory(server);
        connection = factory.newConnection();
        channel = connection.createChannel();

        if (durable) {
            initDurableChannel();
        } else {
            initNotDurableChannel();
        }

        LOGGER.info("RabbitConsumer[{}, {}] connect success !!!", this.server, this.exchangeName);
    }

    private void initDurableChannel() throws Exception {
        channel.exchangeDeclare(this.exchangeName, "fanout", true);
        queueName = channel.queueDeclare(clientId > -1 ? this.exchangeName + "-" + this.clientId : "", true, false, false, null).getQueue();
        channel.queueBind(queueName, this.exchangeName, "");
        channel.basicQos(100);
    }

    private void initNotDurableChannel() throws Exception {
        channel.exchangeDeclare(this.exchangeName, "fanout");
        queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, this.exchangeName, "");
        channel.basicQos(100);
    }

    @Override
    public void start() throws Exception {
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {
                try {
                    if (null != msgQueueHandler) {
                        msgQueueHandler.handle(body);
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        this.channel.basicConsume(this.queueName, false, consumer);
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.close();
            this.connection.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}