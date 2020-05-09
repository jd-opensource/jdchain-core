package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.KVWriteBatch;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.utils.Bytes;
import io.netty.channel.ChannelHandlerContext;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 连接会话
 */
public class KVDBSession implements Session {
    // 会话ID
    private final String id;
    // Channel上下文
    private final ChannelHandlerContext ctx;
    // 当前数据库实例名称
    private String dbName;
    // 当前数据库实例
    private KVDBInstance instance;
    // 批处理模式
    private boolean batchMode;
    // 待提交批处理数据集
    private HashMap<Bytes, byte[]> batch;
    // 最大batch数量
    private static final int MAX_BATCH_SIZE = 10000000;

    public KVDBSession(String id, ChannelHandlerContext ctx) {
        this.id = id;
        this.ctx = ctx;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized void setDB(String dbName, KVDBInstance instance) throws RocksDBException {
        batchAbort();
        this.dbName = dbName;
        this.instance = instance;
    }

    @Override
    public KVDBInstance getDBInstance() {
        return instance;
    }

    @Override
    public String getDBName() {
        return dbName;
    }

    @Override
    public void publish(Message msg) {
        ctx.writeAndFlush(msg);
    }

    @Override
    public void close() {
        if (null != batch) {
            batch.clear();
        }
        if (null != ctx) {
            ctx.close();
        }
    }

    @Override
    public boolean batchMode() {
        return batchMode;
    }

    /**
     * 开启批处理操作，幂等
     *
     * @throws RocksDBException
     */
    @Override
    public synchronized void batchBegin() throws RocksDBException {
        if (batchMode) {
            return;
        }
        batchMode = true;
        if (null != batch) {
            batch.clear();
        } else {
            batch = new HashMap<>();
        }
    }

    /**
     * 取消批处理，幂等
     *
     * @throws RocksDBException
     */
    @Override
    public synchronized void batchAbort() throws RocksDBException {
        batchMode = false;
        if (null != batch) {
            batch.clear();
        }
    }

    /**
     * 提交批处理，执行rocksdb批处理操作
     *
     * @throws RocksDBException
     */
    @Override
    public synchronized void batchCommit() throws RocksDBException {
        batchMode = false;
        if (null != batch) {
            KVWriteBatch writeBatch = instance.beginBatch();
            Iterator<Map.Entry<Bytes, byte[]>> iterator = batch.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Bytes, byte[]> entry = iterator.next();
                writeBatch.set(entry.getKey().toBytes(), entry.getValue());
            }
            writeBatch.commit();
            batch.clear();
        }
    }

    /**
     * 批处理时读钩子，具体操作逻辑由各自executor定义
     *
     * @param hook
     * @return
     * @throws RocksDBException
     */
    @Override
    public byte[] readInBatch(BatchHook hook) throws RocksDBException {
        return hook.exec(batch);
    }

    /**
     * 批处理时读钩子，具体操作逻辑由各自executor定义
     *
     * @param hook
     * @return
     * @throws RocksDBException
     */
    @Override
    public byte[] writeInBatch(BatchHook hook) throws RocksDBException {
        if (batch.size() < MAX_BATCH_SIZE) {
            synchronized (batch) {
                if (batch.size() < MAX_BATCH_SIZE) {
                    return hook.exec(batch);
                }
            }
        }
        throw new KVDBException("executions in this batch is too huge, no more execution allowed");
    }
}
