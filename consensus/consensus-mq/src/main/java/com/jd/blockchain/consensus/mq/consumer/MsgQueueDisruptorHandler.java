package com.jd.blockchain.consensus.mq.consumer;

import com.jd.blockchain.consensus.event.EventEntity;
import com.jd.blockchain.consensus.event.EventProducer;
import com.jd.blockchain.consensus.mq.exchange.BytesEventFactory;
import com.jd.blockchain.consensus.mq.exchange.BytesEventProducer;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class MsgQueueDisruptorHandler implements MsgQueueHandler {

    protected EventProducer eventProducer;

    public MsgQueueDisruptorHandler(EventHandler eventHandler) {
        Disruptor<EventEntity<byte[]>> disruptor =
                new Disruptor<>(new BytesEventFactory(),
                        BytesEventFactory.BUFFER_SIZE, r -> {
                    return new Thread(r);
                }, ProducerType.SINGLE, new BlockingWaitStrategy());

        disruptor.handleEventsWith(eventHandler);
        disruptor.start();
        RingBuffer<EventEntity<byte[]>> ringBuffer = disruptor.getRingBuffer();
        this.eventProducer = new BytesEventProducer(ringBuffer);
    }

    @Override
    public void handle(byte[] msg) {
        this.eventProducer.publish(msg);
    }
}
