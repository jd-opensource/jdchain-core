/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.sdk.nats.RabbitConsumer
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/5 下午10:40
 * Description:
 */
package com.jd.blockchain.consensus.mq.consumer;

import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author shaozhuguang
 * @create 2018/11/5
 * @since 1.0.0
 */

public class NatsConsumer implements MsgQueueConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NatsConsumer.class);
    private final ExecutorService msgListener = Executors.newSingleThreadExecutor();
    private Connection nc;
    private Subscription sub;
    private final String server;
    private final String topic;
    private final int clientId;
    private final boolean durable;
    private MsgQueueHandler msgQueueHandler;

    public NatsConsumer(int clientId, String server, String topic, boolean durable) {
        this.clientId = clientId;
        this.server = server;
        this.topic = topic;
        this.durable = durable;
    }

    @Override
    public void connect(MsgQueueHandler msgQueueHandler) throws Exception {
        this.msgQueueHandler = msgQueueHandler;
        Options options = new Options.Builder().server(server).noReconnect().build();
        this.nc = Nats.connect(options);
        this.sub = nc.subscribe(topic);
        this.nc.flush(Duration.ZERO);
        LOGGER.info("NatsConsumer[{}, {}] connect success !!!", this.server, this.topic);
    }

    @Override
    public void start() {
        msgListener.execute(() -> {
            for (; ; ) {
                try {
                    Message msg = this.sub.nextMessage(Duration.ZERO);
                    msgQueueHandler.handle(msg.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        try {
            nc.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}