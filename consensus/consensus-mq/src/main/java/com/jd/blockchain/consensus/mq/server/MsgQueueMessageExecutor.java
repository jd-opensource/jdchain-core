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
import com.jd.blockchain.consensus.mq.consumer.MsgQueueHandler;
import com.jd.blockchain.consensus.mq.event.BlockMessage;
import com.jd.blockchain.consensus.mq.event.MessageConvertor;
import com.jd.blockchain.consensus.mq.event.TxMessage;
import com.jd.blockchain.consensus.mq.event.TxResultMessage;
import com.jd.blockchain.consensus.mq.exchange.ExchangeEventInnerEntity;
import com.jd.blockchain.consensus.mq.exchange.ExchangeType;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.ledger.TransactionState;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.codec.Base58Utils;
import utils.concurrent.AsyncFuture;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */

public class MsgQueueMessageExecutor implements EventHandler<EventEntity<ExchangeEventInnerEntity>>, MsgQueueHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgQueueMessageExecutor.class);

    // todo 暂不处理队列溢出导致的OOM
    private final ExecutorService blockEventExecutor = Executors.newFixedThreadPool(10);
    private final AtomicInteger messageId = new AtomicInteger();
    private final List<TxMessage> exchangeEvents = new ArrayList<>();
    private MsgQueueProducer txResultProducer;
    private MsgQueueProducer blockProducer;
    private int nodeId;
    private boolean singleNode;
    private boolean isLeader;
    private String realmName;
    private MessageHandle messageHandle;
    private int txSizePerBlock = 1000;

    private StateMachineReplicate stateMachineReplicator;

    public MsgQueueMessageExecutor setRealmName(String realmName) {
        this.realmName = realmName;
        return this;
    }

    public MsgQueueMessageExecutor setTxResultProducer(MsgQueueProducer txResultProducer) {
        this.txResultProducer = txResultProducer;
        return this;
    }

    public MsgQueueMessageExecutor setBlockProducer(MsgQueueProducer blockProducer) {
        this.blockProducer = blockProducer;
        return this;
    }

    public MsgQueueMessageExecutor setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
        return this;
    }

    public MsgQueueMessageExecutor setNodeId(int nodeId, boolean singleNode) {
        this.nodeId = nodeId;
        this.singleNode = singleNode;
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
                txResultProducer.connect();
                if (!singleNode) {
                    blockProducer.connect();
                }
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
            if (entity.getType() == ExchangeType.PROPOSE || entity.getType() == ExchangeType.EMPTY) {
                if (!exchangeEvents.isEmpty()) {
                    process(exchangeEvents);
                    exchangeEvents.clear();
                }
            } else {
                byte[] bytes = event.getEntity().getContent();
                exchangeEvents.add(new TxMessage(MessageConvertor.messageKey(bytes), bytes));
            }
        }
    }

    private void process(List<TxMessage> txMessages) {
        if (txMessages != null && !txMessages.isEmpty()) {
            try {
                Map<String, AsyncFuture<byte[]>> txResponseMap = execute(txMessages);
                if (txResponseMap != null && !txResponseMap.isEmpty()) {
                    for (Map.Entry<String, AsyncFuture<byte[]>> entry : txResponseMap.entrySet()) {
                        final String txKey = entry.getKey();
                        final AsyncFuture<byte[]> asyncFuture = entry.getValue();

                        blockEventExecutor.execute(() -> {
                            TxResultMessage txResultMessage = new TxResultMessage(txKey, asyncFuture.get());
                            try {
                                // 通过消息队列发送交易执行结果
                                LOGGER.debug("publish tx result message, key: {}", txKey);
                                this.txResultProducer.publish(MessageConvertor.serializeTxResultEvent(txResultMessage));
                            } catch (Exception e) {
                                LOGGER.error("publish tx result message exception", e);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                // 打印日志
                LOGGER.error("process tx result message exception", e);
            }
        }
    }

    private Map<String, AsyncFuture<byte[]>> execute(List<TxMessage> txMessages) {
        Map<String, AsyncFuture<byte[]>> asyncFutureMap = new HashMap<>();
        // 使用MessageHandle处理
        MsgQueueConsensusMessageContext consensusContext = MsgQueueConsensusMessageContext.createInstance(realmName);
        String batchId = messageHandle.beginBatch(consensusContext);
        consensusContext.setBatchId(batchId);
        try {
            for (TxMessage txMessage : txMessages) {
                String txKey = txMessage.getKey();
                byte[] txContent = txMessage.getMessage();
                AsyncFuture<byte[]> asyncFuture = messageHandle.processOrdered(messageId.getAndIncrement(), txContent, consensusContext);
                asyncFutureMap.put(txKey, asyncFuture);
            }
            consensusContext.setTimestamp(System.currentTimeMillis());
            StateSnapshot snapshot = messageHandle.completeBatch(consensusContext);
            messageHandle.commitBatch(consensusContext);
            if (isLeader && !singleNode) {
                // 领导者节点向follower同步区块
                this.blockProducer.publish(MessageConvertor.serializeBlockTxs(new BlockMessage(snapshot.getId(), snapshot.getTimestamp(), snapshot.getSnapshot(), txMessages)));
            }
        } catch (Exception e) {
            // todo 需要处理应答码
            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
        }
        return asyncFutureMap;
    }

    private void execute(BlockMessage block) {
        if (block.getHeight() <= stateMachineReplicator.getLatestStateID(realmName)) {
            LOGGER.warn("ignore old block, height: {}, timestamp: {}, hash: {}", block.getHeight(), block.getTimestamp(), Base58Utils.encode(block.getHash()));
            return;
        }
        // 使用MessageHandle处理
        MsgQueueConsensusMessageContext consensusContext = MsgQueueConsensusMessageContext.createInstance(realmName);
        String batchId = messageHandle.beginBatch(consensusContext);
        consensusContext.setBatchId(batchId);
        try {
            for (TxMessage txMessage : block.getTxMessages()) {
                messageHandle.processOrdered(messageId.getAndIncrement(), txMessage.getMessage(), consensusContext);
            }
            consensusContext.setTimestamp(block.getTimestamp());
            StateSnapshot snapshot = messageHandle.completeBatch(consensusContext);
            if (snapshot.getId() == block.getHeight() && Arrays.equals(snapshot.getSnapshot(), block.getHash())) {
                messageHandle.commitBatch(consensusContext);
            } else {
                LOGGER.error("stateSnapshot not match the leader's, \nfollower height: {}, timestamp: {}, hash: {} \nleader: height: {}, timestamp: {}, hash: {}",
                        snapshot.getId(), snapshot.getTimestamp(), Base58Utils.encode(snapshot.getSnapshot()),
                        block.getHeight(), block.getTimestamp(), Base58Utils.encode(block.getHash()));
                messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
            }
        } catch (Exception e) {
            LOGGER.error("follower execute exception", e);
            // todo 需要处理应答码
            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
        }
    }

    @Override
    public void handle(byte[] msg) {
        try {
            execute(MessageConvertor.convertBytesToBlockTxs(msg));
        } catch (Exception e) {
            // 打印日志
            LOGGER.error("process message exception", e);
        }
    }
}