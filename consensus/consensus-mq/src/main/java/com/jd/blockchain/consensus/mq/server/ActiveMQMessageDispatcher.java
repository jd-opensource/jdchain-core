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

public class ActiveMQMessageDispatcher extends AbstractMQMessageDispatcher {

    // 是否可以提议新区块
    private volatile boolean canPropose;

    public ActiveMQMessageDispatcher(
            String server,
            MQServerSettings serverSettings,
            MessageHandle messageHandle,
            StateMachineReplicate stateMachineReplicator) {
        super(server, serverSettings, messageHandle, stateMachineReplicator);
    }

    @Override
    void init() {
        this.proposeExecutor =
                new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("proposer", true));
        this.pingExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ping", true));
        this.resultExecutor =
                new ThreadPoolExecutor(1, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        this.txConsumer = MQFactory.newTxConsumer(nodeId, server, txTopic);
        this.msgConsumer = MQFactory.newMsgConsumer(server, msgTopic);

        this.proposeProducer = MQFactory.newProposeProducer(server, txTopic);
        this.txResultProducer = MQFactory.newTxResultProducer(server, txResultTopic);
        this.msgProducer = MQFactory.newMsgProducer(server, msgTopic);
        this.msgResultProducer = MQFactory.newMsgResultProducer(server, msgResultTopic);
    }

    @Override
    public void onProposeTime() {
        boolean propose = false;
        txLock.lock();
        try {
            if (txMessages.size() > 0) {
                propose = true;
            }
        } finally {
            txLock.unlock();
        }
        if (canPropose && propose) {
            canPropose = false;
            propose();
        }
    }

    @Override
    public void onTx(MQEvent tx) {
        boolean propose = false;
        ProposeEvent block = null;
        List<TxEvent> txBatch = null;
        txLock.lock();
        try {
            if (tx instanceof TxEvent) {
                TxEvent event = (TxEvent) tx;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("receive tx {}", event.getKey());
                }
                txMessages.add(event);
                if (txMessages.size() >= txSizePerBlock) {
                    propose = true;
                }
                canPropose = true;
            } else if (tx instanceof ProposeEvent) {
                block = (ProposeEvent) tx;
                if (txMessages.size() > 0) {
                    txBatch = txMessages;
                    txMessages = new ArrayList<>();
                }
            }
        } finally {
            txLock.unlock();
        }
        if (null != block && null != txBatch && txBatch.size() > 0) {
            onPropose(
                    new ProposeMessage(
                            block.getProposer(),
                            block.getTimestamp(),
                            block.getLatestHeight(),
                            block.getLatestHash(),
                            txBatch));
        } else if (propose) {
            propose();
        }
    }

    @Override
    public synchronized void propose() {
        if (null == proposeProducer) {
            return;
        }
        try {
            ProposeMessage propose =
                    new ProposeMessage(nodeId, System.currentTimeMillis(), latestHeight, latestHash);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("propose {}", propose);
            }
            // Solo模式不推送提议
            if (!singleNode) {
                proposeProducer.publish(BinaryProtocol.encode(propose));
            } else {
                List<TxEvent> txBatch;
                txLock.lock();
                try {
                    txBatch = txMessages;
                    txMessages = new ArrayList<>();
                } finally {
                    txLock.unlock();
                }
                propose.setTxs(txBatch);
                onPropose(propose);
            }
        } catch (Exception e) {
            LOGGER.error("propose error", e);
        }
    }
}
