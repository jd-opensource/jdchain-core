package com.jd.blockchain.consensus.mq;

import com.jd.blockchain.consensus.mq.consumer.ActiveMQConsumer;
import com.jd.blockchain.consensus.mq.consumer.MQConsumer;
import com.jd.blockchain.consensus.mq.consumer.RabbitMQConsumer;
import com.jd.blockchain.consensus.mq.producer.ActiveMQProducer;
import com.jd.blockchain.consensus.mq.producer.MQProducer;
import com.jd.blockchain.consensus.mq.producer.RabbitMQProducer;
import com.jd.blockchain.consensus.mq.server.ActiveMQMessageDispatcher;
import com.jd.blockchain.consensus.mq.server.MQMessageDispatcher;
import com.jd.blockchain.consensus.mq.server.RabbitMQMessageDispatcher;
import com.jd.blockchain.consensus.mq.settings.MQServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateMachineReplicate;

public class MQFactory {

  public static final String RABBITMQ_PREFIX = "rabbitmq://";
  public static final String ACTIVEMQ_PREFIX = "activemq://";

  public static MQMessageDispatcher newQueueDispatcher(
      MQServerSettings settings,
      MessageHandle messageHandle,
      StateMachineReplicate stateMachineReplicator) {
    String server = settings.getConsensusSettings().getNetworkSettings().getServer();
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return new RabbitMQMessageDispatcher(server, settings, messageHandle, stateMachineReplicator);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return new ActiveMQMessageDispatcher(server, settings, messageHandle, stateMachineReplicator);
    }

    return null;
  }

  public static MQProducer newTxProducer(String server, String topic) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQProducer.newQueueProducer(
          getMQServerURI(RABBITMQ_PREFIX, server), topic, false);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQProducer.newTopicProducer(
          getMQServerURI(ACTIVEMQ_PREFIX, server), topic, true);
    }

    return null;
  }

  public static MQConsumer newTxConsumer(int clientId, String server, String topic) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQConsumer.newQueueConsumer(
          clientId, getMQServerURI(RABBITMQ_PREFIX, server), topic, false);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQConsumer.newTopicConsumer(
          clientId, getMQServerURI(ACTIVEMQ_PREFIX, server), topic, true);
    }

    return null;
  }

  public static MQProducer newProposeProducer(String server, String topic) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQProducer.newTopicProducer(
          getMQServerURI(RABBITMQ_PREFIX, server), topic, true);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQProducer.newTopicProducer(
          getMQServerURI(ACTIVEMQ_PREFIX, server), topic, true);
    }

    return null;
  }

  public static MQConsumer newProposeConsumer(int clientId, String server, String topic) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQConsumer.newTopicConsumer(
          clientId, getMQServerURI(RABBITMQ_PREFIX, server), topic, true);
    }

    return null;
  }

  public static MQProducer newTxResultProducer(String server, String topic) {
    return newTopicProducer(server, topic, false);
  }

  public static MQConsumer newTxResultConsumer(String server, String topic) {
    return newTopicConsumer(server, topic, false);
  }

  public static MQProducer newMsgProducer(String server, String topic) {
    return newTopicProducer(server, topic, false);
  }

  public static MQConsumer newMsgConsumer(int clientId, String server, String topic) {
    return newTopicConsumer(clientId, server, topic, false);
  }

  public static MQConsumer newMsgConsumer(String server, String topic) {
    return newTopicConsumer(server, topic, false);
  }

  public static MQProducer newMsgResultProducer(String server, String topic) {
    return newTopicProducer(server, topic, false);
  }

  public static MQConsumer newMsgResultConsumer(String server, String topic) {
    return newTopicConsumer(server, topic, false);
  }

  private static MQProducer newQueueProducer(String server, String queue, boolean durable) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQProducer.newQueueProducer(
          getMQServerURI(RABBITMQ_PREFIX, server), queue, durable);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQProducer.newQueueProducer(
          getMQServerURI(ACTIVEMQ_PREFIX, server), queue, durable);
    }

    return null;
  }

  private static MQProducer newTopicProducer(String server, String topic, boolean durable) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQProducer.newTopicProducer(
          getMQServerURI(RABBITMQ_PREFIX, server), topic, durable);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQProducer.newTopicProducer(
          getMQServerURI(ACTIVEMQ_PREFIX, server), topic, durable);
    }

    return null;
  }

  private static MQConsumer newQueueConsumer(
      int clientId, String server, String queue, boolean durable) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQConsumer.newQueueConsumer(
          clientId, getMQServerURI(RABBITMQ_PREFIX, server), queue, durable);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQConsumer.newQueueConsumer(
          clientId, getMQServerURI(ACTIVEMQ_PREFIX, server), queue);
    }

    return null;
  }

  private static MQConsumer newTopicConsumer(
      int clientId, String server, String topic, boolean durable) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQConsumer.newTopicConsumer(
          clientId, getMQServerURI(RABBITMQ_PREFIX, server), topic, durable);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQConsumer.newTopicConsumer(
          clientId, getMQServerURI(ACTIVEMQ_PREFIX, server), topic, durable);
    }

    return null;
  }

  private static MQConsumer newTopicConsumer(String server, String topic, boolean durable) {
    if (server.startsWith(RABBITMQ_PREFIX)) {
      return RabbitMQConsumer.newTopicConsumer(
          getMQServerURI(RABBITMQ_PREFIX, server), topic, durable);
    } else if (server.startsWith(ACTIVEMQ_PREFIX)) {
      return ActiveMQConsumer.newTopicConsumer(
          getMQServerURI(ACTIVEMQ_PREFIX, server), topic, durable);
    }

    return null;
  }

  private static String getMQServerURI(String prefix, String server) {
    return server.substring(prefix.length());
  }
}
