package com.jd.blockchain.kvdb;

public interface KVStorage extends KVWrite, KVRead {

    /**
     * 开启一个批写入操作； <br>
     * <p>
     * 注：返回的 {@link KVWriteBatch} 实例不是线程安全的，请勿在多个线程之间共享同一个 KVWriteBatch ；
     *
     * @return
     */
    KVWriteBatch beginBatch();

}