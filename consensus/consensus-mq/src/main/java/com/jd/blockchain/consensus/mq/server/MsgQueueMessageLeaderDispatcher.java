package com.jd.blockchain.consensus.mq.server;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.event.EventEntity;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueDisruptorHandler;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueHandler;
import com.jd.blockchain.consensus.mq.event.*;
import com.jd.blockchain.consensus.mq.factory.MsgQueueFactory;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MsgQueueMessageLeaderDispatcher implements MsgQueueMessageDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgQueueMessageLeaderDispatcher.class);
    private MessageHandle messageHandle;
    private MsgQueueServerSettings serverSettings;
    private String realmName;
    private MsgQueueConsumer txConsumer;
    private MsgQueueProducer txResultProducer;
    private MsgQueueProducer blockProducer;
    private MsgQueueConsumer msgConsumer;
    private MsgQueueProducer msgResultProducer;
    private int nodeId;
    private volatile boolean singleNode;

    public MsgQueueMessageLeaderDispatcher(MsgQueueServerSettings serverSettings, MessageHandle messageHandle) {
        this.serverSettings = serverSettings;
        this.realmName = serverSettings.getRealmName();
        this.nodeId = serverSettings.getMsgQueueNodeSettings().getId();
        this.singleNode = serverSettings.getConsensusSettings().getNodes().length == 1;
        this.messageHandle = messageHandle;

        MsgQueueNetworkSettings networkSettings = serverSettings.getConsensusSettings().getNetworkSettings();
        String server = networkSettings.getServer(),
                txTopic = networkSettings.getTxTopic(),
                txResultTopic = networkSettings.getTxResultTopic(),
                blockTopic = networkSettings.getBlockTopic(),
                msgTopic = networkSettings.getMsgTopic(),
                msgResultTopic = networkSettings.getMsgResultTopic();

        this.txConsumer = MsgQueueFactory.newConsumer(nodeId, server, txTopic, false);
        this.msgConsumer = MsgQueueFactory.newConsumer(nodeId, server, msgTopic, false);

        this.txResultProducer = MsgQueueFactory.newProducer(nodeId, server, txResultTopic, false);
        this.blockProducer = MsgQueueFactory.newProducer(nodeId, server, blockTopic, true);
        this.msgResultProducer = MsgQueueFactory.newProducer(nodeId, server, msgResultTopic, false);
    }

    @Override
    public void run() {
        try {
            txConsumer.start();
            msgConsumer.start();
        } catch (Exception e) {
            LOGGER.error("consumers start error", e);
        }
    }

    @Override
    public void connect() throws Exception {
        txConsumer.connect(new MsgQueueDisruptorHandler(new TxEventHandler(serverSettings.getBlockSettings().getTxSizePerBlock(),
                serverSettings.getBlockSettings().getMaxDelayMilliSecondsPerBlock())));
        txResultProducer.connect();
        msgConsumer.connect(new ExtendMessageHandler());
        msgResultProducer.connect();
        blockProducer.connect();
    }

    @Override
    public void close() throws IOException {
        txConsumer.close();
        msgConsumer.close();
        txResultProducer.close();
        msgResultProducer.close();
        blockProducer.close();
    }

    private class TxEventHandler implements EventHandler<EventEntity<byte[]>> {
        private final ScheduledThreadPoolExecutor blockExecutor = new ScheduledThreadPoolExecutor(1);
        private final ExecutorService resultExecutor = Executors.newFixedThreadPool(2);
        private final AtomicInteger messageId = new AtomicInteger();
        private final List<TxMessage> txMessages = new ArrayList<>();
        private final int txSizePerBlock;
        private final long maxDelayMilliSecondsPerBlock;
        private ReentrantLock txLock = new ReentrantLock();

        TxEventHandler(int txSizePerBlock, long maxDelayMilliSecondsPerBlock) {
            this.txSizePerBlock = txSizePerBlock;
            this.maxDelayMilliSecondsPerBlock = maxDelayMilliSecondsPerBlock;
            if (maxDelayMilliSecondsPerBlock > 0) {
                this.blockExecutor.scheduleWithFixedDelay(() -> newBlock(), maxDelayMilliSecondsPerBlock, maxDelayMilliSecondsPerBlock, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void onEvent(EventEntity<byte[]> event, long l, boolean b) throws Exception {
            try {
                byte[] data = event.getEntity();
                int size;
                txLock.lock();
                try {
                    txMessages.add(new TxMessage(MessageConvertor.messageKey(data), data));
                    size = txMessages.size();
                } finally {
                    txLock.unlock();
                }
                if (size >= txSizePerBlock) {
                    newBlock();
                }
            } catch (Exception e) {
                LOGGER.info("dispatcher error", e);
            }
        }

        private void newBlock() {
            txLock.lock();
            if (!txMessages.isEmpty()) {
                try {
                    LOGGER.info("propose new block, tx size: {}", txMessages.size());
                    Map<String, AsyncFuture<byte[]>> txResponseMap = execute(txMessages);
                    if (txResponseMap != null && !txResponseMap.isEmpty()) {
                        for (Map.Entry<String, AsyncFuture<byte[]>> entry : txResponseMap.entrySet()) {
                            final String txKey = entry.getKey();
                            final AsyncFuture<byte[]> asyncFuture = entry.getValue();

                            resultExecutor.execute(() -> {
                                ResultMessage resultMessage = new ResultMessage(txKey, MessageType.TX, asyncFuture.get());
                                try {
                                    // 通过消息队列发送交易执行结果
                                    LOGGER.debug("publish tx result message, key: {}", txKey);
                                    txResultProducer.publish(MessageConvertor.serializeResultEvent(resultMessage));
                                } catch (Exception e) {
                                    LOGGER.error("publish tx result message exception", e);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    // 打印日志
                    LOGGER.error("process tx result message exception", e);
                } finally {
                    txMessages.clear();
                }
            }
            txLock.unlock();
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
                if (!singleNode) {
                    LOGGER.info("publish block message: {}", snapshot.getId());
                    // 领导者节点向follower同步区块
                    blockProducer.publish(MessageConvertor.serializeBlockTxs(new BlockMessage(snapshot.getId(), snapshot.getTimestamp(), snapshot.getSnapshot(), txMessages)));
                }
            } catch (Exception e) {
                TxResponseMessage responseMessage = new TxResponseMessage();
                responseMessage.setExecutionState(TransactionState.IGNORED_BY_BLOCK_FULL_ROLLBACK);
                byte[] encode = BinaryProtocol.encode(responseMessage, TransactionResponse.class);
                for (TxMessage txMessage : txMessages) {
                    String key = txMessage.getKey();
                    try {
                        if (asyncFutureMap.containsKey(key)) {
                            asyncFutureMap.get(key).cancel();
                        }
                    } catch (Exception ex) {
                        LOGGER.error("cancel rollback future error", ex);
                    }
                    CompletableAsyncFuture<byte[]> asyncResult = new CompletableAsyncFuture();
                    asyncResult.complete(encode);
                    asyncFutureMap.put(key, asyncResult);
                }

                messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
            }
            return asyncFutureMap;
        }
    }

    private class ExtendMessageHandler implements MsgQueueHandler {

        @Override
        public void handle(byte[] msg) {
            try {
                singleNode = false;
                // TODO 更新共识信息
                byte[] result = MessageConvertor.serializeExtendMessageResult(ExtendMessageResult.createSuccessResult());
                ResultMessage resultMessage = new ResultMessage(MessageConvertor.messageKey(msg), MessageType.RECONFIGURE, result);
                msgResultProducer.publish(MessageConvertor.serializeResultEvent(resultMessage));
            } catch (Exception e) {
                LOGGER.error("handle extend message error", e);
            }
        }
    }
}