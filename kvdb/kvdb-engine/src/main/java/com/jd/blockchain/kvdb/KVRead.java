package com.jd.blockchain.kvdb;

import org.rocksdb.RocksDBException;

public interface KVRead {

    byte[] get(byte[] key) throws RocksDBException;

    String get(String key) throws RocksDBException;

}