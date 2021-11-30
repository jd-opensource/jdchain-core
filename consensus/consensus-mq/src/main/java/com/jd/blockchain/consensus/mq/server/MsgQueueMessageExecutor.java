/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.server.MsgQueueMessageExecutor
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/13 下午2:10
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.event.EventEntity;
import com.jd.blockchain.consensus.mq.event.MessageEvent;
import com.jd.blockchain.consensus.mq.event.TxBlockedEvent;
import com.jd.blockchain.consensus.mq.exchange.ExchangeEventInnerEntity;
import com.jd.blockchain.consensus.mq.exchange.ExchangeType;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.util.MessageConvertUtil;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.ledger.TransactionState;
import com.lmax.disruptor.EventHandler;

import utils.concurrent.AsyncFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */

public class MsgQueueMessageExecutor implements EventHandler<EventEntity<ExchangeEventInnerEntity>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgQueueMessageExecutor.class);

    // todo 暂不处理队列溢出导致的OOM
    private final ExecutorService blockEventExecutor = Executors.newFixedThreadPool(10);

    private MsgQueueProducer blProducer;

    private MsgQueueProducer preBlProducer;

    private boolean isLeader;

    private List<MessageEvent> exchangeEvents = new ArrayList<>();

    private String realmName;

    private MessageHandle messageHandle;

    private final AtomicInteger messageId = new AtomicInteger();

    private int txSizePerBlock = 1000;

    private StateMachineReplicate stateMachineReplicator;

    public MsgQueueMessageExecutor setRealmName(String realmName) {
        this.realmName = realmName;
        return this;
    }

    public MsgQueueMessageExecutor setBlProducer(MsgQueueProducer blProducer) {
        this.blProducer = blProducer;
        return this;
    }

    public MsgQueueMessageExecutor setPreBlProducer(MsgQueueProducer preBlProducer) {
        this.preBlProducer = preBlProducer;
        return this;
    }

    public MsgQueueMessageExecutor setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
        return this;
    }

    public MsgQueueMessageExecutor setTxSizePerBlock(int txSizePerBlock) {
        this.txSizePerBlock = txSizePerBlock;
        return this;
    }

    public MsgQueueMessageExecutor setMessageHandle(MessageHandle messageHandle) {
        this.messageHandle = messageHandle;
        return this;
    }

    public MsgQueueMessageExecutor setStateMachineReplicator(StateMachineReplicate stateMachineReplicator) {
        this.stateMachineReplicator = stateMachineReplicator;
        return this;
    }

    public MsgQueueMessageExecutor init() {
        try {
            long latestStateId = stateMachineReplicator.getLatestStateID(realmName);
            // 设置基础消息ID
            messageId.set(((int) latestStateId + 1) * txSizePerBlock);
            if (isLeader) {
                blProducer.connect();
                preBlProducer.connect();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public void onEvent(EventEntity<ExchangeEventInnerEntity> event, long sequence, boolean endOfBatch) throws Exception {
        ExchangeEventInnerEntity entity = event.getEntity();
        if (entity != null) {
            if (entity.getType() == ExchangeType.PREBLOCK) {
                process(event.getEntity());
            } else if (entity.getType() == ExchangeType.BLOCK || entity.getType() == ExchangeType.EMPTY) {
                if (!exchangeEvents.isEmpty()) {
                    process(exchangeEvents);
                    exchangeEvents.clear();
                }
            } else {
                byte[] bytes = event.getEntity().getContent();
                String key = bytes2Key(bytes);
                exchangeEvents.add(new MessageEvent(key, bytes));
            }
        }
    }

    private void process(List<MessageEvent> messageEvents) {
        if (messageEvents != null && !messageEvents.isEmpty()) {
            try {
                Map<String, AsyncFuture<byte[]>> txResponseMap = execute(messageEvents);
                if (txResponseMap != null && !txResponseMap.isEmpty()) {
                    if (isLeader) {
                        // 领导者节点向follower同步区块
                        this.preBlProducer.publish(MessageConvertUtil.serializeBlockTxs(messageEvents));
                    }
                    for (Map.Entry<String, AsyncFuture<byte[]>> entry : txResponseMap.entrySet()) {
                        final String txKey = entry.getKey();
                        final AsyncFuture<byte[]> asyncFuture = entry.getValue();

                        blockEventExecutor.execute(() -> {
                            TxBlockedEvent txBlockedEvent = new TxBlockedEvent(txKey,
                                    MessageConvertUtil.base64Encode(asyncFuture.get()));
                            byte[] serializeBytes = MessageConvertUtil.serializeTxBlockedEvent(txBlockedEvent);
                            // 通过消息队列发送该消息
                            try {
                                this.blProducer.publish(serializeBytes);
                            } catch (Exception e) {
                                LOGGER.error("publish block event message exception {}", e.getMessage());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                // 打印日志
                LOGGER.error("process message exception {}", e.getMessage());
            }
        }
    }

    private void process(ExchangeEventInnerEntity message) {
        try {
            execute(MessageConvertUtil.convertBytesToBlockTxs(message.getContent()));
        } catch (Exception e) {
            // 打印日志
            LOGGER.error("process message exception {}", e.getMessage());
        }
    }

    private Map<String, AsyncFuture<byte[]>> execute(List<MessageEvent> messageEvents) {
        Map<String, AsyncFuture<byte[]>> asyncFutureMap = new HashMap<>();
        // 使用MessageHandle处理
        MsgQueueConsensusMessageContext consensusContext = MsgQueueConsensusMessageContext.createInstance(realmName);
        String batchId = messageHandle.beginBatch(consensusContext);
        consensusContext.setBatchId(batchId);
        try {
            for (MessageEvent messageEvent : messageEvents) {
                String txKey = messageEvent.getMessageKey();
                byte[] txContent = messageEvent.getMessage();
                AsyncFuture<byte[]> asyncFuture = messageHandle.processOrdered(messageId.getAndIncrement(), txContent, consensusContext);
                asyncFutureMap.put(txKey, asyncFuture);
            }
            consensusContext.setTimestamp(System.currentTimeMillis());
            messageHandle.completeBatch(consensusContext);
            // TODO imuge 验证
            messageHandle.commitBatch(consensusContext);
        } catch (Exception e) {
            // todo 需要处理应答码
            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
        }
        return asyncFutureMap;
    }


    private String bytes2Key(byte[] bytes) {
        return MessageConvertUtil.messageKey(bytes);
    }
}