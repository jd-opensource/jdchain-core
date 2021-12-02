package com.jd.blockchain.consensus.raft.msgbus;

import com.alipay.sofa.jraft.util.DisruptorBuilder;
import com.alipay.sofa.jraft.util.LogExceptionHandler;
import com.alipay.sofa.jraft.util.NamedThreadFactory;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class MessageBusComponent implements MessageBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusComponent.class);

    private static final int DEFAULT_EXECUTOR_THREAD_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private volatile boolean closed;
    private ReentrantLock lock;

    private Map<String, List<Subcriber>> subcribeMap;
    private Map<String, ExecutorService> executorMap;

    private Disruptor<Message> messageDisruptor;
    private RingBuffer<Message> messageQueue;

    public MessageBusComponent(int ringBufferSize) {
        this.closed = false;
        this.lock = new ReentrantLock();
        this.subcribeMap = new ConcurrentHashMap<>();
        this.executorMap = new ConcurrentHashMap<>();
        this.messageDisruptor = DisruptorBuilder.<Message>newInstance()
                .setRingBufferSize(ringBufferSize)
                .setEventFactory(new MessageFactory())
                .setThreadFactory(new NamedThreadFactory("Message-Bus-Disruptor-", true))
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.messageDisruptor.handleEventsWith(new MessageHandler());
        this.messageDisruptor.setDefaultExceptionHandler(new LogExceptionHandler<Object>(getClass().getSimpleName()));
        this.messageQueue = this.messageDisruptor.start();
    }

    @Override
    public void register(String topic, Subcriber subcriber) {
        lock.lock();
        try {
            if (closed) {
                return;
            }

            if (!subcribeMap.containsKey(topic)) {
                subcribeMap.put(topic, new CopyOnWriteArrayList<>());
                executorMap.put(topic, Executors.newFixedThreadPool(DEFAULT_EXECUTOR_THREAD_SIZE));
            }

            List<Subcriber> subscribeList = subcribeMap.get(topic);
            if (!subscribeList.contains(subcriber)) {
                subscribeList.add(subcriber);
            }

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deregister(String topic, Subcriber subcriber) {
        lock.lock();
        try {

            if (closed) {
                return;
            }

            List<Subcriber> subscribeList = subcribeMap.get(topic);
            if (subscribeList == null || subscribeList.isEmpty()) {
                return;
            }

            subscribeList.remove(subcriber);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void publish(String topic, byte[] data) {
        Message message = new Message(topic, data, false);
        publish(message);
    }

    @Override
    public void publishOrdered(String topic, byte[] data) {
        Message message = new Message(topic, data, true);
        publish(message);
    }

    private void publish(Message message) {
        if (closed) {
            return;
        }

        try {
            final EventTranslator<Message> translator = (event, sequence) -> {
                event.reset();
                event.isOrder = message.isOrder;
                event.topic = message.topic;
                event.data = message.data;
            };

            this.messageQueue.publishEvent(translator);
        } catch (final Exception e) {
            LOGGER.error("fail to publish message.", e);
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            this.closed = true;
            messageDisruptor.shutdown();

            subcribeMap.values().stream().flatMap(Collection::stream)
                    .distinct()
                    .forEach(Subcriber::onQuit);

            executorMap.values()
                    .forEach(ExecutorService::shutdown);

        } finally {
            lock.unlock();
        }
    }


    private static class Message {
        private String topic;
        private byte[] data;
        private boolean isOrder;

        public Message() {
        }

        public Message(String topic, byte[] data, boolean isOrder) {
            this.topic = topic;
            this.isOrder = isOrder;
            this.data = data;
        }

        public void reset() {
            this.topic = null;
            this.data = null;
            this.isOrder = false;
        }
    }

    private static class MessageFactory implements EventFactory<Message> {

        @Override
        public Message newInstance() {
            return new Message();
        }
    }

    private class MessageHandler implements EventHandler<Message> {
        @Override
        public void onEvent(Message msg, long sequence, boolean endBatch) throws Exception {

            if (MessageBusComponent.this.closed) {
                return;
            }

            if (!subcribeMap.containsKey(msg.topic) || !executorMap.containsKey(msg.topic)) {
                LOGGER.error("message topic {} not register", msg.topic);
                return;
            }

            ExecutorService executor = executorMap.get(msg.topic);
            List<Subcriber> subscribeList = subcribeMap.get(msg.topic);

            subscribeList.forEach(s -> {
                if (msg.isOrder) {
                    s.onMessage(msg.data);
                } else {
                    executor.submit(() -> s.onMessage(msg.data));
                }
            });

            msg.reset();
        }
    }
}
