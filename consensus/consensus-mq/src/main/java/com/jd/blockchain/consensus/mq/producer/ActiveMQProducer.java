package com.jd.blockchain.consensus.mq.producer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;

public class ActiveMQProducer implements MQProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQProducer.class);

  private String uri;
  private Connection connection;
  private String topic = "";
  private String queue = "";
  private boolean durable;
  private boolean queueMode;

  private volatile boolean running;
  private ProducerPool producerPool;

  private ActiveMQProducer() {}

  public static ActiveMQProducer newQueueProducer(String uri, String queue, boolean durable) {
    ActiveMQProducer producer = new ActiveMQProducer();
    producer.uri = uri;
    producer.queue = queue;
    producer.durable = durable;
    producer.queueMode = true;
    LOGGER.debug("new queue producer: {}-{}", producer.uri, producer.queue);
    return producer;
  }

  public static ActiveMQProducer newTopicProducer(String uri, String topic, boolean durable) {
    ActiveMQProducer producer = new ActiveMQProducer();
    producer.uri = uri;
    producer.topic = topic;
    producer.durable = durable;
    LOGGER.debug("new topic producer: {}-{}-{}", producer.uri, producer.queue, durable);
    return producer;
  }

  @Override
  public void connect() throws Exception {
    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(uri);
    connectionFactory.setUseAsyncSend(true);
    this.connection = connectionFactory.createConnection();
    this.producerPool = new ProducerPool(connectionFactory.getMaxThreadPoolSize());
    connection.start();
    this.running = true;
  }

  @Override
  public void publish(byte[] message) throws Exception {
    if (running) {
      PooledProducer pooledProducer = null;
      try {
        pooledProducer = producerPool.borrowObject();
        pooledProducer.publish(message);
      } finally {
        if (null != pooledProducer) {
          producerPool.returnObject(pooledProducer);
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    try {
      running = false;
      producerPool.close();
      connection.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private class PooledProducer {
    private Session session;
    private MessageProducer producer;

    public PooledProducer(Session session, MessageProducer producer) {
      this.session = session;
      this.producer = producer;
    }

    public synchronized void publish(byte[] message) throws Exception {
      BytesMessage msg = session.createBytesMessage();
      msg.writeBytes(message);
      this.producer.send(msg);
    }
  }

  private class ProducerPool extends GenericObjectPool<PooledProducer> {

    public ProducerPool(int maxTotal) {
      super(
          new BasePooledObjectFactory<PooledProducer>() {
            @Override
            public PooledProducer create() throws Exception {
              Session session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
              MessageProducer producer;
              if (queueMode) {
                producer = session.createProducer(session.createQueue(queue));
              } else {
                producer = session.createProducer(session.createTopic(topic));
              }
              producer.setDeliveryMode(
                  durable ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
              return new PooledProducer(session, producer);
            }

            @Override
            public PooledObject<PooledProducer> wrap(PooledProducer producer) {
              return new DefaultPooledObject(producer);
            }

            @Override
            public void destroyObject(PooledObject<PooledProducer> p, DestroyMode destroyMode)
                throws Exception {
              super.destroyObject(p, destroyMode);
              PooledProducer producer = p.getObject();
              if (null != producer) {
                producer.session.close();
              }
            }
          });

      GenericObjectPoolConfig<PooledProducer> poolConfig = new GenericObjectPoolConfig<>();
      poolConfig.setMaxTotal(maxTotal);
      poolConfig.setMaxIdle(maxTotal);
      poolConfig.setMinIdle(0);
      setConfig(poolConfig);
    }
  }
}
