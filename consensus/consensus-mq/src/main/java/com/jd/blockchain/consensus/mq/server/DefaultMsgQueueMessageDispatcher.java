/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.server.DefaultMsgQueueMessageDispatcher
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/13 上午11:05
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.event.EventEntity;
import com.jd.blockchain.consensus.event.EventProducer;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.exchange.ExchangeEntityFactory;
import com.jd.blockchain.consensus.mq.exchange.ExchangeEventFactory;
import com.jd.blockchain.consensus.mq.exchange.ExchangeEventInnerEntity;
import com.jd.blockchain.consensus.mq.exchange.ExchangeEventProducer;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */

public class DefaultMsgQueueMessageDispatcher implements MsgQueueMessageDispatcher, EventHandler<EventEntity<byte[]>> {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultMsgQueueMessageDispatcher.class);

    private static final byte[] blockCommitBytes = new byte[]{0x00};

    private final ScheduledThreadPoolExecutor timeHandleExecutor = new ScheduledThreadPoolExecutor(2);

    private final AtomicLong blockIndex = new AtomicLong();

    private long syncIndex = 0L;

    private MsgQueueProducer txProducer;

    private MsgQueueConsumer txConsumer;

    private MsgQueueConsumer blockConsumer;

    private EventProducer eventProducer;

    private EventHandler eventHandler;

    private final int TX_SIZE_PER_BLOCK;

    private final long MAX_DELAY_MILLISECONDS_PER_BLOCK;

    private boolean isRunning;

    private boolean isLeader;

    private boolean isConnected;

    public DefaultMsgQueueMessageDispatcher(int txSizePerBlock, long maxDelayMilliSecondsPerBlock) {
        this.TX_SIZE_PER_BLOCK = txSizePerBlock;
        this.MAX_DELAY_MILLISECONDS_PER_BLOCK = maxDelayMilliSecondsPerBlock;
    }

    public DefaultMsgQueueMessageDispatcher setTxProducer(MsgQueueProducer txProducer) {
        this.txProducer = txProducer;
        return this;
    }

    public DefaultMsgQueueMessageDispatcher setTxConsumer(MsgQueueConsumer txConsumer) {
        this.txConsumer = txConsumer;
        return this;
    }

    public DefaultMsgQueueMessageDispatcher setBlockConsumer(MsgQueueConsumer blockConsumer) {
        this.blockConsumer = blockConsumer;
        return this;
    }

    public DefaultMsgQueueMessageDispatcher setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
        return this;
    }

    public DefaultMsgQueueMessageDispatcher setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
        return this;
    }

    public void init() {
        handleDisruptor(eventHandler);
    }

    private void handleDisruptor(EventHandler eventHandler) {
        Disruptor<EventEntity<ExchangeEventInnerEntity>> disruptor =
                new Disruptor<>(new ExchangeEventFactory(),
                        ExchangeEventFactory.BUFFER_SIZE, r -> {
                    return new Thread(r);
                }, ProducerType.SINGLE, new BlockingWaitStrategy());

        disruptor.handleEventsWith(eventHandler);
        disruptor.start();
        RingBuffer<EventEntity<ExchangeEventInnerEntity>> ringBuffer = disruptor.getRingBuffer();

        this.eventProducer = new ExchangeEventProducer(ringBuffer);
    }

    public synchronized void connect() throws Exception {
        if (!isConnected) {
            if (isLeader) {
                txProducer.connect();
                txConsumer.connect(this);
            } else {
                blockConsumer.connect(this);
            }
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
        try {
            if (isLeader) {
                txConsumer.start();
            } else {
                blockConsumer.start();
            }
        } catch (Exception e) {

        }
    }

    private Runnable timeBlockTask(final long currentBlockIndex) {
        return () -> {
            final boolean isEqualBlock = this.blockIndex.compareAndSet(
                    currentBlockIndex, currentBlockIndex + 1);
            if (isEqualBlock) {
                try {
                    txProducer.publish(blockCommitBytes);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (isLeader) {
            this.txProducer.close();
            this.txConsumer.close();
        } else {
            this.blockConsumer.close();
        }
    }

    @Override
    public void onEvent(EventEntity<byte[]> event, long sequence, boolean endOfBatch) throws Exception {
        try {
            if (isLeader) {
                byte[] data = event.getEntity();
                if (data.length == 1) {
                    // 结块标识优先处理
                    syncIndex = 0L;
                    this.blockIndex.getAndIncrement();
                    LOGGER.info("propose new block for timer");
                    eventProducer.publish(ExchangeEntityFactory.newProposeInstance());
                } else {
                    if (syncIndex == 0) { // 收到第一个交易
                        // 需要判断是否需要进行定时任务
                        if (MAX_DELAY_MILLISECONDS_PER_BLOCK > 0) {
                            this.timeHandleExecutor.schedule(
                                    timeBlockTask(this.blockIndex.get()),
                                    MAX_DELAY_MILLISECONDS_PER_BLOCK, TimeUnit.MILLISECONDS);
                        }
                    }
                    syncIndex++;
                    eventProducer.publish(ExchangeEntityFactory.newTransactionInstance(data));
                    if (syncIndex == TX_SIZE_PER_BLOCK) {
                        syncIndex = 0L;
                        this.blockIndex.getAndIncrement();
                        LOGGER.info("propose new block for txs");
                        eventProducer.publish(ExchangeEntityFactory.newProposeInstance());
                    }
                }
            } else {
                LOGGER.info("receive new block");
                eventProducer.publish(ExchangeEntityFactory.newBlockInstance(event.getEntity()));
            }
        } catch (Exception e) {
            LOGGER.info("dispatcher error", e);
        }
    }
}