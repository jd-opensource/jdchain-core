package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import org.rocksdb.RocksDBException;

/**
 * 连接会话
 */
public interface Session {

    /**
     * 会话ID
     *
     * @return
     */
    String getId();

    /**
     * 设置当前会话使用的数据库实例名称
     *
     * @param dbName
     * @param instance
     * @throws RocksDBException
     */
    void setDB(String dbName, KVDBInstance instance) throws RocksDBException;

    /**
     * @return 数据库实例
     */
    KVDBInstance getDBInstance();

    /**
     * @return 数据库名称
     */
    String getDBName();

    /**
     * 返送消息
     *
     * @param msg
     */
    void publish(Message msg);

    /**
     * 关闭
     */
    void close();

    /**
     * @return 是否处于批处理模式
     */
    boolean batchMode();

    /**
     * 开启批处理
     *
     * @throws RocksDBException
     */
    void batchBegin() throws RocksDBException;

    /**
     * 取消批处理
     *
     * @throws RocksDBException
     */
    void batchAbort() throws RocksDBException;

    /**
     * 提交批处理
     *
     * @throws RocksDBException
     */
    void batchCommit() throws RocksDBException;

    /**
     * 批处理时读钩子方法
     *
     * @param hook
     * @return
     * @throws RocksDBException
     */
    byte[] readInBatch(BatchHook hook) throws RocksDBException;

    /**
     * 批处理时写钩子方法
     *
     * @param hook
     * @return
     * @throws RocksDBException
     */
    byte[] writeInBatch(BatchHook hook) throws RocksDBException;
}
