package com.jd.blockchain.consensus.mq.consumer;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RabbitMQConsumer implements MQConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);
  private String uri;
  private String topic = "";
  private String queue = "";
  private int clientId = -1;
  private boolean durable;
  private Connection connection;
  private Channel channel;
  private boolean queueMode;

  private volatile boolean running;
  private DefaultConsumer consumer;

  private RabbitMQConsumer() {}

  public static RabbitMQConsumer newQueueConsumer(
      int clientId, String uri, String queue, boolean durable) {
    RabbitMQConsumer consumer = new RabbitMQConsumer();
    consumer.uri = uri;
    consumer.queue = queue;
    consumer.clientId = clientId;
    consumer.queueMode = true;
    consumer.durable = durable;
    LOGGER.debug("new queue consumer: {}-{}", consumer.queue, consumer.clientId);
    return consumer;
  }

  public static RabbitMQConsumer newTopicConsumer(
      int clientId, String uri, String topic, boolean durable) {
    RabbitMQConsumer consumer = new RabbitMQConsumer();
    consumer.uri = uri;
    consumer.topic = topic;
    consumer.clientId = clientId;
    consumer.durable = durable;
    LOGGER.debug("new topic consumer: {}-{}", consumer.topic, consumer.clientId);
    return consumer;
  }

  public static RabbitMQConsumer newTopicConsumer(String uri, String topic, boolean durable) {
    RabbitMQConsumer consumer = new RabbitMQConsumer();
    consumer.uri = uri;
    consumer.topic = topic;
    consumer.durable = durable;
    LOGGER.debug("new topic consumer: {}-{}", consumer.topic, consumer.clientId);
    return consumer;
  }

  @Override
  public void connect(MQHandler msgQueueHandler) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri(uri);
    connection = factory.newConnection();
    channel = connection.createChannel();

    if (queueMode) {
      initQueueChannel();
    } else {
      initExchangeChannel();
    }

    if (null != msgQueueHandler) {
      this.consumer =
          new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                String consumerTag,
                Envelope envelope,
                AMQP.BasicProperties properties,
                byte[] body) {
              try {
                if (running && null != msgQueueHandler) {
                  msgQueueHandler.handle(body);
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          };
    }
  }

  private void initQueueChannel() throws Exception {
    channel.queueDeclare(this.queue, durable, false, true, null);
  }

  private void initExchangeChannel() throws Exception {
    channel.exchangeDeclare(this.topic, BuiltinExchangeType.FANOUT, durable);
    if (durable) {
      queue =
          channel
              .queueDeclare(
                  clientId > -1 ? this.topic + "-" + this.clientId : "", true, false, false, null)
              .getQueue();
    } else {
      queue = channel.queueDeclare().getQueue();
    }
    channel.queueBind(queue, this.topic, "");
    channel.basicQos(100);
  }

  @Override
  public void start() throws Exception {
    this.running = true;
    this.channel.basicConsume(this.queue, true, consumer);
  }

  @Override
  public void close() throws IOException {
    try {
      this.running = false;
      this.channel.close();
      this.connection.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
