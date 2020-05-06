package com.jd.blockchain.kvdb;

import org.rocksdb.RocksDBException;

public interface KVWriteBatch extends KVWrite {

    void commit() throws RocksDBException;

}
