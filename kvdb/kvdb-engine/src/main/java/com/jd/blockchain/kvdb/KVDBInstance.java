package com.jd.blockchain.kvdb;

import com.jd.blockchain.utils.io.BytesUtils;
import org.rocksdb.RocksDBException;

public abstract class KVDBInstance implements KVStorage {

    @Override
    public String get(String key) throws RocksDBException {
        return BytesUtils.toString(get(BytesUtils.toBytes(key)));
    }

    @Override
    public void set(String key, String value) throws RocksDBException {
        set(BytesUtils.toBytes(key), BytesUtils.toBytes(value));
    }

    /**
     * 关闭数据库；<p>
     * <p>
     * 注：关闭过程中可能引发的异常将被处理而不会被抛出；
     */
    public abstract void close();

    /**
     * 移除数据库；<p>
     * <p>
     * 注：移除过程中可能引发的异常将被处理而不会被抛出；
     */
    public abstract void drop();
}