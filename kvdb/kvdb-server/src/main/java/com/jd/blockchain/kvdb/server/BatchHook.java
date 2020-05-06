package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.utils.Bytes;
import org.rocksdb.RocksDBException;

import java.util.HashMap;

/**
 * 批处理钩子
 */
public interface BatchHook {

    byte[] exec(HashMap<Bytes, byte[]> wb) throws RocksDBException;

}
