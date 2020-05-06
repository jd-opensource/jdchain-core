package com.jd.blockchain.kvdb.client;

import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.utils.Bytes;

/**
 * KVDB SDK 数据操作
 */
public interface KVDBOperator {

    /**
     * 存在性查询
     *
     * @param key
     * @return
     * @throws KVDBException
     */
    boolean exists(Bytes key) throws KVDBException;

    /**
     * 存在性查询，支持多个键值
     *
     * @param keys
     * @return
     * @throws KVDBException
     */
    boolean[] exists(Bytes... keys) throws KVDBException;

    /**
     * 键值获取
     *
     * @param key
     * @return
     * @throws KVDBException
     */
    Bytes get(Bytes key) throws KVDBException;

    /**
     * 键值获取，支持多个键值
     *
     * @param keys
     * @return
     * @throws KVDBException
     */
    Bytes[] get(Bytes... keys) throws KVDBException;

    /**
     * 设置键值对
     *
     * @param key
     * @param value
     * @return
     * @throws KVDBException
     */
    boolean put(Bytes key, Bytes value) throws KVDBException;

    /**
     * 开启批处理
     *
     * @return
     * @throws KVDBException
     */
    boolean batchBegin() throws KVDBException;

    /**
     * 取消批处理
     *
     * @return
     * @throws KVDBException
     */
    boolean batchAbort() throws KVDBException;

    /**
     * 提交批处理，服务器掉线重连后会丢失未提交批处理数据
     * <p>
     * 未提交的`batch`对其他客户端连接不可见。
     *
     * @return
     * @throws KVDBException
     */
    boolean batchCommit() throws KVDBException;
}
