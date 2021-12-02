/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.server.DefaultMsgQueueMessageDispatcher
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/13 上午11:05
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueHandler;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.service.MessageHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */

public class ExtendMsgQueueQueueMessageExecutor implements MsgQueueMessageDispatcher, MsgQueueHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendMsgQueueQueueMessageExecutor.class);

    private final ExecutorService dataExecutor = Executors.newSingleThreadExecutor();

    private MsgQueueProducer msgProducer;

    private MsgQueueConsumer msgConsumer;

    private MessageHandle messageHandle;

    private boolean isRunning;

    private boolean isConnected;

    public ExtendMsgQueueQueueMessageExecutor setMsgProducer(MsgQueueProducer msgProducer) {
        this.msgProducer = msgProducer;
        return this;
    }

    public ExtendMsgQueueQueueMessageExecutor setMsgConsumer(MsgQueueConsumer msgConsumer) {
        this.msgConsumer = msgConsumer;
        return this;
    }

    public ExtendMsgQueueQueueMessageExecutor setMessageHandle(MessageHandle messageHandle) {
        this.messageHandle = messageHandle;
        return this;
    }

    @Override
    public void init() {
        // do nothing
    }

    public synchronized void connect() throws Exception {
        if (!isConnected) {
            msgProducer.connect();
            msgConsumer.connect(this);
            msgConsumer.start();
            isConnected = true;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        isRunning = false;
        close();
    }

    @Override
    public void run() {
        this.isRunning = true;
    }

    @Override
    public void close() throws IOException {
        isConnected = false;
        this.msgProducer.close();
        this.msgConsumer.close();
    }

    @Override
    public void handle(byte[] msg) {
        dataExecutor.execute(() -> {
            try {
                AsyncFuture<byte[]> result = messageHandle.processUnordered(msg);
                msgProducer.publish(result.get());
            } catch (Exception e) {
                LOGGER.error("process Unordered message exception", e);
            }
        });
    }
}