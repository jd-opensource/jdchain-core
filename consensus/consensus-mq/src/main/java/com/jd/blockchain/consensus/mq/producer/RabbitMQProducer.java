package com.jd.blockchain.consensus.mq.producer;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RabbitMQProducer implements MQProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQProducer.class);

  private String uri;
  private Channel channel;
  private Connection connection;
  private String topic = "";
  private String queue = "";
  private boolean durable;
  private boolean queueMode;

  private volatile boolean running;

  private RabbitMQProducer() {}

  public static RabbitMQProducer newQueueProducer(String uri, String queue, boolean durable) {
    RabbitMQProducer producer = new RabbitMQProducer();
    producer.uri = uri;
    producer.queue = queue;
    producer.durable = durable;
    producer.queueMode = true;
    LOGGER.debug("new queue producer: {}-{}", producer.uri, producer.queue);
    return producer;
  }

  public static RabbitMQProducer newTopicProducer(String uri, String topic, boolean durable) {
    RabbitMQProducer producer = new RabbitMQProducer();
    producer.uri = uri;
    producer.topic = topic;
    producer.durable = durable;
    LOGGER.debug("new topic producer: {}-{}-{}", producer.uri, producer.queue, durable);
    return producer;
  }

  @Override
  public void connect() throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri(uri);
    connection = factory.newConnection();
    channel = connection.createChannel();
    if (!queueMode) {
      channel.exchangeDeclare(this.topic, BuiltinExchangeType.FANOUT, durable);
    } else {
      channel.queueDeclare(this.queue, durable, false, true, null);
    }
    running = true;
  }

  @Override
  public void publish(byte[] message) throws Exception {
    if (running) {
      channel.basicPublish(this.topic, queue, MessageProperties.PERSISTENT_TEXT_PLAIN, message);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      running = false;
      channel.close();
      connection.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
