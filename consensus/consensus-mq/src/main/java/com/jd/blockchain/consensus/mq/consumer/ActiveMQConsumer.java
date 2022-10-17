package com.jd.blockchain.consensus.mq.consumer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.util.UUID;

public class ActiveMQConsumer implements MQConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConsumer.class);

  private String clientId;
  private String uri;
  private Connection connection;
  private Session session;
  private String topic = "";
  private String queue = "";
  private boolean durable;
  private boolean queueMode;

  private volatile boolean running;
  private MessageConsumer consumer;

  private ActiveMQConsumer() {}

  public static ActiveMQConsumer newQueueConsumer(int clientId, String uri, String queue) {
    ActiveMQConsumer consumer = new ActiveMQConsumer();
    consumer.uri = uri;
    consumer.queue = queue;
    consumer.clientId = clientId + "";
    consumer.queueMode = true;
    LOGGER.debug("new queue consumer: {}-{}", consumer.queue, consumer.clientId);
    return consumer;
  }

  public static ActiveMQConsumer newTopicConsumer(
      int clientId, String uri, String topic, boolean durable) {
    ActiveMQConsumer consumer = new ActiveMQConsumer();
    consumer.uri = uri;
    consumer.topic = topic;
    consumer.clientId = clientId + "";
    consumer.durable = durable;
    LOGGER.debug("new topic consumer: {}-{}", consumer.topic, consumer.clientId);
    return consumer;
  }

  public static ActiveMQConsumer newTopicConsumer(String uri, String topic, boolean durable) {
    ActiveMQConsumer consumer = new ActiveMQConsumer();
    consumer.uri = uri;
    consumer.topic = topic;
    consumer.clientId = UUID.randomUUID().toString();
    consumer.durable = durable;
    LOGGER.debug("new topic consumer: {}-{}", consumer.topic, consumer.clientId);
    return consumer;
  }

  @Override
  public void connect(MQHandler msgHandler) throws Exception {
    ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(uri);
    this.connection = connectionFactory.createConnection();
    if (queueMode) {
      this.session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
      this.consumer = session.createConsumer(session.createQueue(queue));
    } else {
      connection.setClientID(clientId + "");
      this.session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
      if (durable) {
        this.consumer = session.createDurableSubscriber(session.createTopic(topic), clientId + "");
      } else {
        this.consumer = session.createConsumer(session.createTopic(topic));
      }
    }
    if (null != msgHandler) {
      consumer.setMessageListener(
          message -> {
            if (running) {
              try {
                BytesMessage msg = (BytesMessage) message;
                byte[] data = new byte[(int) msg.getBodyLength()];
                msg.readBytes(data);
                msgHandler.handle(data);
              } catch (JMSException e) {
                LOGGER.warn("queue message error", e);
              }
            }
          });
    }
  }

  @Override
  public void start() throws Exception {
    running = true;
    connection.start();
  }

  @Override
  public void close() throws IOException {
    try {
      running = false;
      session.close();
      connection.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
