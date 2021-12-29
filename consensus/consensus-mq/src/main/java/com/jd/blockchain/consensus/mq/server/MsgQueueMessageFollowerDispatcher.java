package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueHandler;
import com.jd.blockchain.consensus.mq.event.BlockMessage;
import com.jd.blockchain.consensus.mq.event.MessageConvertor;
import com.jd.blockchain.consensus.mq.event.TxMessage;
import com.jd.blockchain.consensus.mq.factory.MsgQueueFactory;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.ledger.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.codec.Base58Utils;

import java.io.IOException;
import java.util.Arrays;


public class MsgQueueMessageFollowerDispatcher implements MsgQueueMessageDispatcher {

    private final Logger LOGGER = LoggerFactory.getLogger(MsgQueueMessageFollowerDispatcher.class);

    private MessageHandle messageHandle;
    private MsgQueueServerSettings serverSettings;
    private String realmName;
    private MsgQueueConsumer blockConsumer;
    private int nodeId;
    private StateMachineReplicate stateMachineReplicator;

    public MsgQueueMessageFollowerDispatcher(MsgQueueServerSettings serverSettings, MessageHandle messageHandle, StateMachineReplicate stateMachineReplicator) {
        this.serverSettings = serverSettings;
        this.realmName = serverSettings.getRealmName();
        this.nodeId = serverSettings.getMsgQueueNodeSettings().getId();
        this.messageHandle = messageHandle;
        this.stateMachineReplicator = stateMachineReplicator;

        MsgQueueNetworkSettings networkSettings = serverSettings.getConsensusSettings().getNetworkSettings();
        String server = networkSettings.getServer(),
                blockTopic = networkSettings.getBlockTopic();

        this.blockConsumer = MsgQueueFactory.newConsumer(nodeId, server, blockTopic, true);
    }

    @Override
    public void connect() throws Exception {
        blockConsumer.connect(new BlockMessageHandler(stateMachineReplicator.getLatestStateID(realmName)));
    }

    @Override
    public void close() throws IOException {
        blockConsumer.close();
    }

    @Override
    public void run() {
        try {
            blockConsumer.start();
        } catch (Exception e) {
            LOGGER.error("consumer start error", e);
        }
    }

    private class BlockMessageHandler implements MsgQueueHandler {

        private long latestHeight;

        BlockMessageHandler(long latestHeight) {
            this.latestHeight = latestHeight;
        }

        @Override
        public void handle(byte[] msg) {
            try {
                BlockMessage block = MessageConvertor.convertBytesToBlockTxs(msg);
                if (block.getHeight() <= latestHeight) {
                    LOGGER.warn("ignore old block, height: {}, timestamp: {}, hash: {}", block.getHeight(), block.getTimestamp(), Base58Utils.encode(block.getHash()));
                    return;
                }
                // 使用MessageHandle处理
                MsgQueueConsensusMessageContext consensusContext = MsgQueueConsensusMessageContext.createInstance(realmName);
                String batchId = messageHandle.beginBatch(consensusContext);
                consensusContext.setBatchId(batchId);
                try {
                    for (TxMessage txMessage : block.getTxMessages()) {
                        messageHandle.processOrdered(-1, txMessage.getMessage(), consensusContext);
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
                    messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
                }
            } catch (Exception e) {
                // 打印日志
                LOGGER.error("process message exception", e);
            }
        }
    }
}