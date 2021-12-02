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
import com.jd.blockchain.consensus.mq.event.TxResultMessage;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.event.MessageConvertor;
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

public class DefaultMessageTransmitter implements MessageTransmitter, MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageTransmitter.class);

    private final ExecutorService messageExecutorArray = Executors.newFixedThreadPool(10);

    private final Map<String, MessageListener> messageListeners = new ConcurrentHashMap<>();

    private final MsgQueueHandler blockMsgQueueHandler = new BlockMsgQueueHandler();

    private final MsgQueueHandler extendMsgQueueHandler = new ExtendMsgQueueHandler();

    private MsgQueueProducer txProducer;

    private MsgQueueProducer msgProducer;

    private MsgQueueConsumer txResultConsumer;

    private MsgQueueConsumer msgConsumer;

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

    public DefaultMessageTransmitter setMsgConsumer(MsgQueueConsumer msgConsumer) {
        this.msgConsumer = msgConsumer;
        return this;
    }

    @Override
    public AsyncFuture<byte[]> sendOrdered(byte[] message) {

        AsyncFuture<byte[]> messageFuture;

        try {
            publishMessage(txProducer, message);
            messageFuture = messageHandle(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return messageFuture;
    }

    @Override
    public AsyncFuture<byte[]> sendUnordered(byte[] message) {
        AsyncFuture<byte[]> messageFuture;
        try {
            publishMessage(msgProducer, message);
            messageFuture = messageHandle(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return messageFuture;
    }

    @Override
    public void connect() throws Exception {
        if (!isConnected) {
            this.txProducer.connect();
            this.txResultConsumer.connect(blockMsgQueueHandler);
            this.msgProducer.connect();
            this.msgConsumer.connect(extendMsgQueueHandler);
            isConnected = true;
            txResultConsumer.start();
            msgConsumer.start();
        }
    }

    @Override
    public void publishMessage(MsgQueueProducer producer, byte[] message) throws Exception {
        producer.publish(message);
    }

    @Override
    public void close() {
        try {
            txProducer.close();
            txResultConsumer.close();
            msgProducer.close();
            msgConsumer.close();
            isConnected = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AsyncFuture<byte[]> messageHandle(byte[] message) throws Exception {
//      异步回调
//      需要监听MQ结块的应答
//      首先需要一个Consumer，在子类已实现
        AsyncFuture<byte[]> messageFuture = registerMessageListener(MessageConvertor.messageKey(message));
        return messageFuture;
    }

    private AsyncFuture<byte[]> registerMessageListener(String messageKey) {
        CompletableAsyncFuture<byte[]> future = new CompletableAsyncFuture<>();
        MessageListener messageListener = new MessageListener(messageKey, future);
        messageListener.addListener();
        return future;
    }

    private boolean isTxBlockedEventNeedManage(final TxResultMessage txResultMessage) {
        if (this.messageListeners.isEmpty()) {
            return false;
        }
        return messageListeners.containsKey(txResultMessage.getKey());
        // 无须处理区块高度
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

        public void received(final TxResultMessage txResultMessage) {
            // 期望是false，假设是false则设置为true，成功的情况下表示是第一次
            byte[] txResp = txResultMessage.getResponse();
            if (txResp != null) {
                if (isDeal.compareAndSet(false, true)) {
                    //生成对应的交易应答
                    future.complete(txResp);
                }
            }
        }

        public void received(final byte[] message) {
            // 期望是false，假设是false则设置为true，成功的情况下表示是第一次
            if (message != null) {
                if (isDeal.compareAndSet(false, true)) {
                    //生成对应的交易应答
                    future.complete(message);
                }
            }
        }
    }

    public class BlockMsgQueueHandler implements MsgQueueHandler {

        @Override
        public void handle(byte[] msg) {
            messageExecutorArray.execute(() -> {
                if (!messageListeners.isEmpty()) {
                    TxResultMessage txResultMessage = MessageConvertor.convertBytesToTxResultEvent(msg);
                    if (txResultMessage != null) {
                        String txKey = txResultMessage.getKey();
                        LOGGER.debug("receive tx result message, key: {}", txKey);
                        // 需要判断该区块是否需要处理
                        if (isTxBlockedEventNeedManage(txResultMessage)) {
                            MessageListener txListener = messageListeners.get(txKey);
                            if (txListener != null) {
                                txListener.received(txResultMessage);
                                messageListeners.remove(txKey);
                            }
                        }
                    }
                }
            });
        }
    }

    public class ExtendMsgQueueHandler implements MsgQueueHandler {

        @Override
        public void handle(byte[] msg) {
            messageExecutorArray.execute(() -> {
                String messageKey = MessageConvertor.messageKey(msg);
                if (messageListeners.containsKey(messageKey)) {
                    MessageListener txListener = messageListeners.get(messageKey);
                    if (txListener != null) {
                        txListener.received(msg);
                        messageListeners.remove(messageKey);
                    }
                }
            });
        }
    }
}