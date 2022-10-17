package com.jd.blockchain.consensus.mq.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.mq.consumer.MQConsumer;
import com.jd.blockchain.consensus.mq.event.ExtendMessage;
import com.jd.blockchain.consensus.mq.event.TxMessage;
import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendResult;
import com.jd.blockchain.consensus.mq.event.binaryproto.MQEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.TxResult;
import com.jd.blockchain.consensus.mq.producer.MQProducer;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MQMessageTransmitter implements MessageTransmitter, MessageService, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MQMessageTransmitter.class);

    private final ExecutorService messageExecutor =
            new ThreadPoolExecutor(
                    1,
                    2 * Runtime.getRuntime().availableProcessors(),
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());

    private final Cache<String, CompletableAsyncFuture> messageFuture =
            CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).build();

    private MQProducer txProducer;
    private MQProducer msgProducer;
    private MQConsumer txResultConsumer;
    private MQConsumer msgResultConsumer;

    private boolean isConnected = false;

    public MQMessageTransmitter setTxProducer(MQProducer txProducer) {
        this.txProducer = txProducer;
        return this;
    }

    public MQMessageTransmitter setMsgProducer(MQProducer msgProducer) {
        this.msgProducer = msgProducer;
        return this;
    }

    public MQMessageTransmitter setTxResultConsumer(MQConsumer txResultConsumer) {
        this.txResultConsumer = txResultConsumer;
        return this;
    }

    public MQMessageTransmitter setMsgResultConsumer(MQConsumer msgResultConsumer) {
        this.msgResultConsumer = msgResultConsumer;
        return this;
    }

    @Override
    public AsyncFuture<byte[]> sendOrdered(byte[] message) {
        try {
            String msgKey = messageKey();
            CompletableAsyncFuture<byte[]> future = new CompletableAsyncFuture<>();
            messageFuture.put(msgKey, future);
            publishMessage(txProducer, new TxMessage(msgKey, message));
            return future;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AsyncFuture<byte[]> sendUnordered(byte[] message) {
        try {
            String msgKey = messageKey();
            CompletableAsyncFuture<byte[]> future = new CompletableAsyncFuture<>();
            messageFuture.put(msgKey, future);
            publishMessage(msgProducer, new ExtendMessage(msgKey, message));
            return future;
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
                this.txResultConsumer.connect(
                        msg -> {
                            try {
                                TxResult resultMessage = BinaryProtocol.decode(msg);
                                if (resultMessage != null) {
                                    String msgKey = resultMessage.getKey();
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("receive tx result message, key: {}", msgKey);
                                    }
                                    CompletableAsyncFuture future = messageFuture.getIfPresent(msgKey);
                                    if (future != null) {
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("complete tx result message, key: {}", msgKey);
                                        }
                                        future.complete(resultMessage.getResult());
                                    }
                                    messageFuture.invalidate(msgKey);
                                }
                            } catch (Exception e) {
                                LOGGER.error("handle tx result message error", e);
                            }
                        });
            }
            if (null != msgProducer) {
                this.msgProducer.connect();
            }
            if (null != msgResultConsumer) {
                this.msgResultConsumer.connect(
                        msg -> {
                            try {
                                ExtendResult resultMessage = BinaryProtocol.decode(msg);
                                if (resultMessage != null) {
                                    String msgKey = resultMessage.getKey();
                                    LOGGER.debug("receive extend result message, key: {}", msgKey);
                                    CompletableAsyncFuture future = messageFuture.getIfPresent(msgKey);
                                    if (future != null) {
                                        future.complete(resultMessage.getResult());
                                    }
                                    messageFuture.invalidate(msgKey);
                                }
                            } catch (Exception e) {
                                LOGGER.error("handle extend result message error", e);
                            }
                        });
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
    public void publishMessage(MQProducer producer, MQEvent event) throws Exception {
        producer.publish(BinaryProtocol.encode(event));
    }

    @Override
    public void close() {
        try {
            messageExecutor.shutdownNow();
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

    private static String messageKey() {
        return UUID.randomUUID().toString();
    }
}
