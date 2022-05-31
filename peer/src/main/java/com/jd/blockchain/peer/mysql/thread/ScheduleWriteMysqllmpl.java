package com.jd.blockchain.peer.mysql.thread;

import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.peer.mysql.service.MapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/27 3:03 PM
 * Version 1.0
 */
public class ScheduleWriteMysqllmpl implements ScheduleWriteMysql {

    private static final long WRITE_MYSQL_TASK_DELAY = 2000;

    private static final long WRITE_MYSQL_TASK_TIMEOUT = 2000;

    private static final Lock writeMysqlLock = new ReentrantLock();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    private static final Logger logger = LoggerFactory.getLogger(ScheduleWriteMysqllmpl.class);

    @Override
    public void init(LedgerQuery ledgerQuery, MapperService mapperService) {
        try {
            logger.info("[ScheduleWriteMysqllmpl]-------Start write mysql task!-------");
            scheduledExecutorService.scheduleWithFixedDelay(new WriteMysqlTask(ledgerQuery, mapperService), WRITE_MYSQL_TASK_DELAY,
                    WRITE_MYSQL_TASK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.info("ScheduleWriteMysqllmpl exception occur, error = " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    // 写mysql的定时任务
    private class WriteMysqlTask implements Runnable {

        LedgerQuery ledgerQuery;

        MapperService mapperService;

        int WRITE_BATCH_SIZE = 10;

        public WriteMysqlTask(LedgerQuery ledgerQuery, MapperService mapperService) {
            this.ledgerQuery = ledgerQuery;
            this.mapperService = mapperService;
        }

        @Override
        public void run() {

            long mysqlBlockHeight;

            long endBlockHeight;

            try {

                // 防止多账本同时操作数据库，造成数据不一致
                writeMysqlLock.lock();

                // 查询目前已入库的区块高度信息
                mysqlBlockHeight = mapperService.getBlockTotal(ledgerQuery.getHash());

                if (mysqlBlockHeight < ledgerQuery.getLatestBlockHeight()) {
                    if (mysqlBlockHeight + WRITE_BATCH_SIZE < ledgerQuery.getLatestBlockHeight()) {
                        endBlockHeight = mysqlBlockHeight + WRITE_BATCH_SIZE;
                    } else {
                        endBlockHeight = ledgerQuery.getLatestBlockHeight();
                    }

                    for (long i = mysqlBlockHeight + 1; i < endBlockHeight + 1; i++) {
                        mapperService.writeAppToMysql(ledgerQuery, i);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("[ScheduleWriteMysqllmpl] write mysql task exception occur! error = " + e.getMessage());
            } finally {
                writeMysqlLock.unlock();
            }
        }
    }

}
