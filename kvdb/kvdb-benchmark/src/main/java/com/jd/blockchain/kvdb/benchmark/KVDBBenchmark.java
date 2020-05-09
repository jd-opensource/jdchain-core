package com.jd.blockchain.kvdb.benchmark;

import com.jd.blockchain.kvdb.client.KVDBClient;
import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.utils.ArgumentSet;
import com.jd.blockchain.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class KVDBBenchmark {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBBenchmark.class);

    private static final String HOST = "-h";
    private static final String PORT = "-p";
    private static final String DB = "-db";
    private static final String CLIENTS = "-c";
    private static final String REQUESTS = "-n";
    private static final String BATCH = "-b";
    private static final String KEEPALIVE = "-k";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 7078;
    private static final int DEFAULT_CLIENT = 20;
    private static final int DEFAULT_REQUESTS = 100000;
    private static final boolean DEFAULT_BATCH = false;
    private static final boolean DEFAULT_KEEP_ALIVE = true;

    private String host;
    private int port;
    private String db;
    private int clients;
    private int requests;
    private boolean batch;
    private boolean keepAlive;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getClients() {
        return clients;
    }

    public void setClients(int clients) {
        this.clients = clients;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public boolean isBatch() {
        return batch;
    }

    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    public boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public KVDBBenchmark(String[] args) {
        ArgumentSet arguments = ArgumentSet.resolve(args, ArgumentSet.setting().prefix(HOST, PORT, CLIENTS, REQUESTS, KEEPALIVE, BATCH, DB));
        ArgumentSet.ArgEntry hostArg = arguments.getArg(HOST);
        if (null != hostArg) {
            this.host = hostArg.getValue();
        } else {
            this.host = DEFAULT_HOST;
        }
        ArgumentSet.ArgEntry portArg = arguments.getArg(PORT);
        if (null != portArg) {
            this.port = Integer.valueOf(portArg.getValue());
        } else {
            this.port = DEFAULT_PORT;
        }
        ArgumentSet.ArgEntry clientsArg = arguments.getArg(CLIENTS);
        if (null != clientsArg) {
            this.clients = Integer.valueOf(clientsArg.getValue());
        } else {
            this.clients = DEFAULT_CLIENT;
        }
        ArgumentSet.ArgEntry requestsArg = arguments.getArg(REQUESTS);
        if (null != requestsArg) {
            this.requests = Integer.valueOf(requestsArg.getValue());
        } else {
            this.requests = DEFAULT_REQUESTS;
        }
        ArgumentSet.ArgEntry kaArg = arguments.getArg(KEEPALIVE);
        if (null != kaArg) {
            this.keepAlive = Boolean.valueOf(kaArg.getValue());
        } else {
            this.keepAlive = DEFAULT_KEEP_ALIVE;
        }
        ArgumentSet.ArgEntry batchArg = arguments.getArg(BATCH);
        if (null != batchArg) {
            this.batch = Boolean.valueOf(batchArg.getValue());
        } else {
            this.batch = DEFAULT_BATCH;
        }
        ArgumentSet.ArgEntry dbArg = arguments.getArg(DB);
        if (null == dbArg) {
            System.out.println("please set -db parameter");
            System.exit(0);
        } else {
            this.db = dbArg.getValue();
        }
    }

    public static void main(String[] args) {
        KVDBBenchmark bm = new KVDBBenchmark(args);
        ClientConfig config = new ClientConfig(bm.getHost(), bm.getPort(), bm.getDb());
        config.setKeepAlive(bm.keepAlive);
        AtomicLong requests = new AtomicLong(bm.requests);
        AtomicLong failCount = new AtomicLong(0);
        CountDownLatch startCdl = new CountDownLatch(1);
        CountDownLatch endCdl = new CountDownLatch(bm.getClients());
        for (int i = 0; i < bm.getClients(); i++) {
            final int index = i;
            new Thread(() -> {
                KVDBClient client = new KVDBClient(config);
                if (bm.batch && bm.keepAlive) {
                    client.batchBegin();
                }
                try {
                    startCdl.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int j = 0;
                while (requests.getAndDecrement() > 0) {
                    try {
                        client.put(Bytes.fromString(index + ":" + j), Bytes.fromInt(1));
                    } catch (KVDBException e) {
                        failCount.incrementAndGet();
                        LOGGER.error("put error", e);
                    }
                }
                if (bm.batch && bm.keepAlive) {
                    client.batchCommit();
                }
                endCdl.countDown();
                client.close();
            }).start();
        }

        long startTime = System.currentTimeMillis();
        startCdl.countDown();
        try {
            endCdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        String result = String.format("requests:%d, clients:%d, batch:%s, times:%dms, errors:%d, tps:%f",
                bm.getRequests(), bm.getClients(), bm.batch, endTime - startTime, failCount.get(), bm.getRequests() / ((endTime - startTime) / 1000d));
        LOGGER.info(result);
        System.out.println(result);

    }

}
