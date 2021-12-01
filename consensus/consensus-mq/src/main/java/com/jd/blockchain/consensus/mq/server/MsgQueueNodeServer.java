/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.server.MsgQueueNodeServer
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/13 上午11:20
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import java.util.concurrent.Executors;

import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.factory.MsgQueueFactory;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.settings.*;
import com.jd.blockchain.consensus.service.Communication;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.NodeState;
import com.jd.blockchain.consensus.service.StateMachineReplicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

/**
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */

public class MsgQueueNodeServer implements NodeServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgQueueNodeServer.class);

    private DefaultMsgQueueMessageDispatcher dispatcher;

    private ExtendMsgQueueMessageExecutor extendExecutor;

    private MessageHandle messageHandle;

    private StateMachineReplicate stateMachineReplicator;

    private MsgQueueMessageExecutor messageExecutor;

    private MsgQueueNetworkSettings networkSettings;

    private MsgQueueConsensusManageService manageService;

    private int txSizePerBlock = 1000;

    private long maxDelayMilliSecondsPerBlock = 1000;

    private MsgQueueServerSettings serverSettings;

    private boolean isRunning;

    private int nodeId;

    public MsgQueueNodeServer setMessageHandle(MessageHandle messageHandle) {
        this.messageHandle = messageHandle;
        return this;
    }

    public MsgQueueNodeServer setStateMachineReplicator(StateMachineReplicate stateMachineReplicator) {
        this.stateMachineReplicator = stateMachineReplicator;
        return this;
    }

    public MsgQueueNodeServer setTxSizePerBlock(int txSizePerBlock) {
        this.txSizePerBlock = txSizePerBlock;
        return this;
    }

    public MsgQueueNodeServer setMaxDelayMilliSecondsPerBlock(long maxDelayMilliSecondsPerBlock) {
        this.maxDelayMilliSecondsPerBlock = maxDelayMilliSecondsPerBlock;
        return this;
    }

    public MsgQueueNodeServer setMsgQueueNetworkSettings(MsgQueueNetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
        return this;
    }

    public MsgQueueNodeServer setServerSettings(MsgQueueServerSettings serverSettings) {
        this.serverSettings = serverSettings;
        this.manageService = new MsgQueueConsensusManageService()
                .setConsensusSettings(serverSettings.getConsensusSettings());
        return this;
    }

    public MsgQueueNodeServer init() {
        MsgQueueNodeSettings currentNodeSettings = serverSettings.getMsgQueueNodeSettings();
        String realmName = this.serverSettings.getRealmName();
        MsgQueueBlockSettings blockSettings = this.serverSettings.getBlockSettings();
        MsgQueueConsensusSettings consensusSettings = this.serverSettings.getConsensusSettings();

        this.setTxSizePerBlock(blockSettings.getTxSizePerBlock())
                .setMaxDelayMilliSecondsPerBlock(blockSettings.getMaxDelayMilliSecondsPerBlock())
                .setMsgQueueNetworkSettings(consensusSettings.getNetworkSettings())
        ;

        this.nodeId = currentNodeSettings.getId();
        LOGGER.info("My nodeId is {}, isLeader: {}", nodeId, isLeader());

        String server = networkSettings.getServer(),
                txTopic = networkSettings.getTxTopic(),
                blTopic = networkSettings.getBlTopic(),
                preBlTopic = networkSettings.getPreBlTopic(),
                msgTopic = networkSettings.getMsgTopic();

        MsgQueueProducer blProducer = MsgQueueFactory.newProducer(nodeId, server, blTopic, true),
                preBlProducer = MsgQueueFactory.newProducer(nodeId, server, preBlTopic, true),
                txProducer = MsgQueueFactory.newProducer(nodeId, server, txTopic, true),
                msgProducer = MsgQueueFactory.newProducer(server, msgTopic, false);

        MsgQueueConsumer txConsumer = MsgQueueFactory.newConsumer(nodeId, server, txTopic, true),
                preBlConsumer = MsgQueueFactory.newConsumer(nodeId, server, preBlTopic, true),
                msgConsumer = MsgQueueFactory.newConsumer(server, msgTopic, false);

        initMessageExecutor(blProducer, preBlProducer, realmName);

        initDispatcher(txProducer, txConsumer, preBlConsumer);

        initExtendExecutor(msgProducer, msgConsumer);

        return this;
    }

    private boolean isLeader() {
        // TODO imuge 此处暂时使用简单等于0判断是否为领导者
        return nodeId == 0;
    }

    @Override
    public String getProviderName() {
        return MsgQueueConsensusProvider.NAME;
    }

    @Override
    public ClientAuthencationService getClientAuthencationService() {
        return this.manageService;
    }

    @Override
    public MsgQueueServerSettings getServerSettings() {
        return serverSettings;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public synchronized AsyncFuture<?> start() {
        if (isRunning) {
            return CompletableAsyncFuture.completeFuture(null);
        }

        isRunning = true;
        CompletableAsyncFuture<?> future = new CompletableAsyncFuture<>();
        Thread thrd = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    dispatcher.connect();
                    Executors.newSingleThreadExecutor().execute(dispatcher);
                    extendExecutor.connect();
                    Executors.newSingleThreadExecutor().execute(extendExecutor);
                    isRunning = true;

                    future.complete(null);
                } catch (Exception e) {
                    isRunning = false;
                    future.error(e);
                }
            }
        });

        thrd.setDaemon(true);
        thrd.start();
        return future;
    }

    @Override
    public synchronized void stop() {
        if (isRunning) {
            try {
                dispatcher.stop();
                extendExecutor.stop();
                isRunning = false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void initMessageExecutor(MsgQueueProducer blProducer, MsgQueueProducer preBlProducer, final String realmName) {
        messageExecutor = new MsgQueueMessageExecutor()
                .setRealmName(realmName)
                .setMessageHandle(messageHandle)
                .setBlProducer(blProducer)
                .setPreBlProducer(preBlProducer)
                .setIsLeader(isLeader())
                .setNodeId(nodeId)
                .setStateMachineReplicator(stateMachineReplicator)
                .setTxSizePerBlock(txSizePerBlock)
                .init()
        ;
    }

    private void initDispatcher(MsgQueueProducer txProducer, MsgQueueConsumer txConsumer, MsgQueueConsumer preBlConsumer) {
        dispatcher = new DefaultMsgQueueMessageDispatcher(txSizePerBlock, maxDelayMilliSecondsPerBlock)
                .setTxProducer(txProducer)
                .setTxConsumer(txConsumer)
                .setPreBlConsumer(preBlConsumer)
                .setIsLeader(isLeader())
                .setEventHandler(messageExecutor)
        ;
        dispatcher.init();
    }


    private void initExtendExecutor(MsgQueueProducer msgProducer, MsgQueueConsumer msgConsumer) {
        extendExecutor = new ExtendMsgQueueMessageExecutor()
                .setMessageHandle(messageHandle)
                .setMsgConsumer(msgConsumer)
                .setMsgProducer(msgProducer)
        ;
        extendExecutor.init();
    }

    @Override
    public NodeState getState() {
        // TODO Auto-generated method stub
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public Communication getCommunication() {
        // TODO Auto-generated method stub
        throw new IllegalStateException("Not implemented!");
    }
}