/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.client.DefaultMessageTransmitter
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午3:05
 * Description:
 */
package com.jd.blockchain.consensus.mq.client;

import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueHandler;
import com.jd.blockchain.consensus.mq.event.MessageConvertor;
import com.jd.blockchain.consensus.mq.event.ResultMessage;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class DefaultMessageTransmitter implements MessageTransmitter, MessageService, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageTransmitter.class);

    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(10);

    private final Map<String, MessageListener> messageListeners = new ConcurrentHashMap<>();

    private final MsgQueueHandler blockMsgQueueHandler = new BlockMsgQueueHandler();

    private final MsgQueueHandler extendMsgQueueHandler = new ExtendMsgQueueHandler();

    private MsgQueueProducer txProducer;

    private MsgQueueProducer msgProducer;

    private MsgQueueConsumer txResultConsumer;

    private MsgQueueConsumer msgResultConsumer;

    private boolean isConnected = false;

    public DefaultMessageTransmitter setTxProducer(MsgQueueProducer txProducer) {
        this.txProducer = txProducer;
        return this;
    }

    public DefaultMessageTransmitter setMsgProducer(MsgQueueProducer msgProducer) {
        this.msgProducer = msgProducer;
        return this;
    }

    public DefaultMessageTransmitter setTxResultConsumer(MsgQueueConsumer txResultConsumer) {
        this.txResultConsumer = txResultConsumer;
        return this;
    }

    public DefaultMessageTransmitter setMsgResultConsumer(MsgQueueConsumer msgResultConsumer) {
        this.msgResultConsumer = msgResultConsumer;
        return this;
    }

    @Override
    public AsyncFuture<byte[]> sendOrdered(byte[] message) {
        try {
            publishMessage(txProducer, message);
            return messageHandle(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AsyncFuture<byte[]> sendUnordered(byte[] message) {
        try {
            publishMessage(msgProducer, message);
            return messageHandle(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connect() throws Exception {
        if (!isConnected) {
            if (null != txProducer) {
                this.txProducer.connect();
            }
            if (null != txResultConsumer) {
                this.txResultConsumer.connect(blockMsgQueueHandler);
            }
            if (null != msgProducer) {
                this.msgProducer.connect();
            }
            if (null != msgResultConsumer) {
                this.msgResultConsumer.connect(extendMsgQueueHandler);
            }
            if (null != txResultConsumer) {
                txResultConsumer.start();
            }
            if (null != msgResultConsumer) {
                msgResultConsumer.start();
            }
            isConnected = true;
        }
    }

    @Override
    public void publishMessage(MsgQueueProducer producer, byte[] message) throws Exception {
        producer.publish(message);
    }

    @Override
    public void close() {
        try {
            if (null != txProducer) {
                txProducer.close();
            }
            if (null != txResultConsumer) {
                txResultConsumer.close();
            }
            if (null != msgProducer) {
                msgProducer.close();
            }
            if (null != msgResultConsumer) {
                msgResultConsumer.close();
            }
            isConnected = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AsyncFuture<byte[]> messageHandle(byte[] message) throws Exception {
        String msgKey = MessageConvertor.messageKey(message);
        LOGGER.debug("register tx result listener, key: {}", msgKey);
        return registerMessageListener(msgKey);
    }

    private AsyncFuture<byte[]> registerMessageListener(String messageKey) {
        CompletableAsyncFuture<byte[]> future = new CompletableAsyncFuture<>();
        MessageListener messageListener = new MessageListener(messageKey, future);
        messageListener.addListener();
        return future;
    }

    private class MessageListener {

        final String messageKey;

        final CompletableAsyncFuture<byte[]> future;

        final AtomicBoolean isDeal = new AtomicBoolean(false);

        public MessageListener(String messageKey, CompletableAsyncFuture<byte[]> future) {
            this.messageKey = messageKey;
            this.future = future;
            addListener();
        }

        public void addListener() {
            synchronized (messageListeners) {
                messageListeners.put(messageKey, this);
            }
        }

        public void received(final ResultMessage resultMessage) {
            // 期望是false，假设是false则设置为true，成功的情况下表示是第一次
            byte[] txResp = resultMessage.getResult();
            if (txResp != null) {
                if (isDeal.compareAndSet(false, true)) {
                    //生成对应的交易应答
                    future.complete(txResp);
                }
            }
        }


    }

    public class BlockMsgQueueHandler implements MsgQueueHandler {

        @Override
        public void handle(byte[] msg) {
            messageExecutor.execute(() -> {
                if (!messageListeners.isEmpty()) {
                    try {
                        ResultMessage resultMessage = MessageConvertor.convertBytesToResultEvent(msg);
                        if (resultMessage != null) {
                            String msgKey = resultMessage.getKey();
                            LOGGER.debug("receive tx result message, key: {}", msgKey);
                            MessageListener listener = messageListeners.get(msgKey);
                            if (listener != null) {
                                messageListeners.remove(msgKey);
                                listener.received(resultMessage);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("handle tx result message error", e);
                    }
                }
            });
        }
    }

    public class ExtendMsgQueueHandler implements MsgQueueHandler {

        @Override
        public void handle(byte[] msg) {
            messageExecutor.execute(() -> {
                if (!messageListeners.isEmpty()) {
                    try {
                        ResultMessage resultMessage = MessageConvertor.convertBytesToResultEvent(msg);
                        if (resultMessage != null) {
                            String msgKey = resultMessage.getKey();
                            LOGGER.debug("receive extend result message, key: {}", msgKey);
                            MessageListener listener = messageListeners.get(msgKey);
                            if (listener != null) {
                                messageListeners.remove(msgKey);
                                listener.received(resultMessage);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("handle extend result message error", e);
                    }
                }
            });
        }
    }
}