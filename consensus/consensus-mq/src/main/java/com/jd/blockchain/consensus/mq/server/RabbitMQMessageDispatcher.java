package com.jd.blockchain.consensus.mq.server;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.mq.MQFactory;
import com.jd.blockchain.consensus.mq.event.ProposeMessage;
import com.jd.blockchain.consensus.mq.event.binaryproto.MQEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.ProposeEvent;
import com.jd.blockchain.consensus.mq.event.binaryproto.TxEvent;
import com.jd.blockchain.consensus.mq.settings.MQServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import utils.concurrent.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RabbitMQMessageDispatcher extends AbstractMQMessageDispatcher {

    public RabbitMQMessageDispatcher(
            String server,
            MQServerSettings serverSettings,
            MessageHandle messageHandle,
            StateMachineReplicate stateMachineReplicator) {
        super(server, serverSettings, messageHandle, stateMachineReplicator);
    }

    @Override
    void init() {
        // RabbitMQ 实现支持第一个节点出块
        if (nodeId == 0) {
            this.proposeExecutor =
                    new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("proposer", true));
            this.resultExecutor =
                    new ThreadPoolExecutor(1, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

            this.txConsumer = MQFactory.newTxConsumer(nodeId, server, txTopic);

            this.proposeProducer = MQFactory.newProposeProducer(server, proposeTopic);
            this.txResultProducer = MQFactory.newTxResultProducer(server, txResultTopic);
        }

        this.pingExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ping", true));

        this.proposeConsumer = MQFactory.newProposeConsumer(nodeId, server, proposeTopic);
        this.msgConsumer = MQFactory.newMsgConsumer(server, msgTopic);
        this.msgProducer = MQFactory.newMsgProducer(server, msgTopic);
        this.msgResultProducer = MQFactory.newMsgResultProducer(server, msgResultTopic);
    }

    @Override
    public void onProposeTime() {
        List<TxEvent> txBatch = null;
        txLock.lock();
        try {
            if (txMessages.size() > 0) {
                txBatch = txMessages;
                txMessages = new ArrayList<>();
            }
        } finally {
            txLock.unlock();
        }
        if (null != txBatch && txBatch.size() > 0) {
            propose(txBatch);
        }
    }

    @Override
    public void onTx(MQEvent event) {
        List<TxEvent> txBatch = null;
        txLock.lock();
        try {
            TxEvent tx = (TxEvent) event;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("receive tx {}", tx.getKey());
            }
            txMessages.add(tx);
            if (txMessages.size() >= txSizePerBlock) {
                txBatch = txMessages;
                txMessages = new ArrayList<>();
            }
        } finally {
            txLock.unlock();
        }
        if (null != txBatch && txBatch.size() > 0) {
            propose(txBatch);
        }
    }

    @Override
    public void propose(List<TxEvent> txs) {
        try {
            ProposeEvent propose =
                    new ProposeMessage(nodeId, System.currentTimeMillis(), latestHeight, latestHash, txs);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("propose {}", propose);
            }
            // Solo模式不推送提议
            if (!singleNode) {
                proposeProducer.publish(BinaryProtocol.encode(propose));
            } else {
                onPropose(propose);
            }
        } catch (Exception e) {
            LOGGER.error("propose error", e);
        }
    }
}
