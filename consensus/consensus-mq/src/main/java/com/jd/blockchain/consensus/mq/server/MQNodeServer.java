package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.mq.MQFactory;
import com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider;
import com.jd.blockchain.consensus.mq.settings.MQServerSettings;
import com.jd.blockchain.consensus.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

import java.util.concurrent.Executors;

public class MQNodeServer implements NodeServer {

    private static Logger LOGGER = LoggerFactory.getLogger(MQNodeServer.class);
    private final MQMessageDispatcher dispatcher;
    private final MQConsensusManageService manageService;
    private final MQServerSettings serverSettings;
    private boolean isRunning;

    public MQNodeServer(
            MQServerSettings serverSettings,
            MessageHandle messageHandler,
            StateMachineReplicate stateMachineReplicator) {
        this.serverSettings = serverSettings;
        this.manageService =
                new MQConsensusManageService().setConsensusSettings(serverSettings.getConsensusSettings());

        this.dispatcher =
                MQFactory.newQueueDispatcher(serverSettings, messageHandler, stateMachineReplicator);
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
    public MQServerSettings getServerSettings() {
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
        Thread thread =
                new Thread(
                        () -> {
                            try {
                                dispatcher.connect();
                                Executors.newSingleThreadExecutor().execute(dispatcher);
                                isRunning = true;
                                future.complete(null);
                            } catch (Exception e) {
                                LOGGER.error("server start error", e);
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
