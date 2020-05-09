package com.jd.blockchain.kvdb.client;

import com.jd.blockchain.kvdb.client.partition.Partitioner;
import com.jd.blockchain.kvdb.client.partition.SimpleMurmur3HashPartitioner;
import com.jd.blockchain.kvdb.protocol.client.NettyClient;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 集群数据库操作
 */
public class KVDBCluster implements KVDBOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBCluster.class);
    // 线程池，用于异步多数据库连接的数据操作
    private ExecutorService executor;
    // 服务器分片器
    private Partitioner partition;
    // 集群节点数据库实例实际操作对象
    private KVDBSingle[] operators;

    public KVDBCluster(NettyClient[] clients) throws KVDBException {
        if (null != clients && clients.length > 0) {
            operators = new KVDBSingle[clients.length];
            int i = 0;
            for (NettyClient client : clients) {
                operators[i] = new KVDBSingle(client);
                i++;
            }
            partition = new SimpleMurmur3HashPartitioner(operators.length);
            executor = Executors.newFixedThreadPool(operators.length);
        } else {
            throw new KVDBException("no cluster config present");
        }
    }

    @Override
    public boolean exists(Bytes key) throws KVDBException {
        return operators[partition.partition(key.toBytes())].exists(key);
    }

    /**
     * TODO multiple keys optimization
     *
     * @param keys
     * @return
     * @throws KVDBException
     */
    @Override
    public boolean[] exists(Bytes... keys) throws KVDBException {
        boolean[] oks = new boolean[keys.length];
        int i = 0;
        for (Bytes key : keys) {
            oks[i] = operators[partition.partition(key.toBytes())].exists(key);
            i++;
        }
        return oks;
    }

    @Override
    public Bytes get(Bytes key) throws KVDBException {
        return operators[partition.partition(key.toBytes())].get(key);
    }

    /**
     * TODO multiple keys optimization
     *
     * @param keys
     * @return
     * @throws KVDBException
     */
    @Override
    public Bytes[] get(Bytes... keys) throws KVDBException {
        Bytes[] vs = new Bytes[keys.length];
        int i = 0;
        for (Bytes key : keys) {
            vs[i] = operators[partition.partition(key.toBytes())].get(key);
            i++;
        }
        return vs;
    }

    /**
     * @param key
     * @param value
     * @return
     * @throws KVDBException
     */
    @Override
    public boolean put(Bytes key, Bytes value) throws KVDBException {
        return operators[partition.partition(key.toBytes())].put(key, value);
    }

    @Override
    public boolean batchBegin() throws KVDBException {
        CountDownLatch cdl = new CountDownLatch(operators.length);
        for (int i = 0; i < operators.length; i++) {
            final int index = i;
            executor.execute(() -> {
                try {
                    operators[index].batchBegin();
                    cdl.countDown();
                } catch (KVDBException e) {
                    LOGGER.error("cluster batchBegin error", e);
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            LOGGER.error("cluster batchBegin error", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean batchAbort() throws KVDBException {
        CountDownLatch cdl = new CountDownLatch(operators.length);
        for (int i = 0; i < operators.length; i++) {
            final int index = i;
            executor.execute(() -> {
                try {
                    operators[index].batchAbort();
                    cdl.countDown();
                } catch (KVDBException e) {
                    LOGGER.error("cluster batchAbort error", e);
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            LOGGER.error("cluster batchAbort error", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean batchCommit() throws KVDBException {
        CountDownLatch cdl = new CountDownLatch(operators.length);
        for (int i = 0; i < operators.length; i++) {
            final int index = i;
            executor.execute(() -> {
                try {
                    operators[index].batchCommit();
                    cdl.countDown();
                } catch (KVDBException e) {
                    LOGGER.error("cluster batchCommit error", e);
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            LOGGER.error("cluster batchCommit error", e);
            return false;
        }
        return true;
    }
}
