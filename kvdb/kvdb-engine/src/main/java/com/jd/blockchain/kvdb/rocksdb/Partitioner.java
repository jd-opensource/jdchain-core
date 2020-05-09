package com.jd.blockchain.kvdb.rocksdb;

/**
 * 分区计算器；
 *
 * @author huanghaiquan
 */
public interface Partitioner {

    /**
     * 总的分区数；
     *
     * @return
     */
    int getPartitionCount();

    /**
     * 计算 key 的分区地址；
     *
     * @param key 要计算的 key；
     * @return 分区号；从 0 （含）到 {@link #getPartitionCount()} (不含）；
     */
    int partition(byte[] key);

}
