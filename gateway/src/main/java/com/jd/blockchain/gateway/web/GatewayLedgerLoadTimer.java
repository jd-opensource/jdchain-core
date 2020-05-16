/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.gateway.web.GatewayTimeTasks
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2019/1/16 下午6:17
 * Description:
 */
package com.jd.blockchain.gateway.web;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jd.blockchain.gateway.PeerConnector;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author shaozhuguang
 * @create 2019/1/16
 * @since 1.0.0
 */
@Component
@EnableScheduling
public class GatewayLedgerLoadTimer {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GatewayLedgerLoadTimer.class);

    private static final ExecutorService ledgerLoadExecutor = initLedgerLoadExecutor();

    private static final Lock lock = new ReentrantLock();

    @Autowired
    private PeerConnector peerConnector;

    //每1钟执行一次
    @Scheduled(cron = "0 */1 * * * * ")
    public void ledgerLoad(){
        lock.lock();
        try {
            // 单线程重连
            ledgerLoadExecutor.execute(() -> {
                peerConnector.reconnect();
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private static ThreadPoolExecutor initLedgerLoadExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("gateway-ledger-loader-%d").build();

        return new ThreadPoolExecutor(1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}