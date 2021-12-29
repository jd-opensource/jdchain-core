package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider;
import com.jd.blockchain.consensus.mq.settings.MsgQueueServerSettings;
import com.jd.blockchain.consensus.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

import java.util.concurrent.Executors;

public class MsgQueueNodeServer implements NodeServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsgQueueNodeServer.class);

    private final MsgQueueMessageDispatcher dispatcher;
    private final MessageHandle messageHandle;
    private final StateMachineReplicate stateMachineReplicator;
    private final MsgQueueConsensusManageService manageService;
    private final MsgQueueServerSettings serverSettings;
    private boolean isRunning;

    public MsgQueueNodeServer(MsgQueueServerSettings serverSettings, MessageHandle messageHandler, StateMachineReplicate stateMachineReplicator) {
        this.serverSettings = serverSettings;
        this.manageService = new MsgQueueConsensusManageService().setConsensusSettings(serverSettings.getConsensusSettings());
        this.messageHandle = messageHandler;
        this.stateMachineReplicator = stateMachineReplicator;

        int nodeId = serverSettings.getMsgQueueNodeSettings().getId();
        if (nodeId == 0) {
            dispatcher = new MsgQueueMessageLeaderDispatcher(serverSettings, messageHandle);
        } else {
            dispatcher = new MsgQueueMessageFollowerDispatcher(serverSettings, messageHandle, stateMachineReplicator);
        }
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
        Thread thread = new Thread(() -> {
            try {
                dispatcher.connect();
                Executors.newSingleThreadExecutor().execute(dispatcher);
                isRunning = true;
                future.complete(null);
            } catch (Exception e) {
                isRunning = false;
                future.error(e);
            }
        });

        thread.setDaemon(true);
        thread.start();
        return future;
    }

    @Override
    public synchronized void stop() {
        if (isRunning) {
            try {
                dispatcher.close();
                isRunning = false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public NodeState getState() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public Communication getCommunication() {
        throw new IllegalStateException("Not implemented!");
    }
}