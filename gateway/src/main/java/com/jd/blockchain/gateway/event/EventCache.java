package com.jd.blockchain.gateway.event;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.Event;

/**
 * 事件缓存
 *
 * @author shaozhuguang
 *
 */
public interface EventCache {

    /**
     * 账本Hash
     *
     * @return
     */
    HashDigest getLedgerHash();

    /**
     * 返回所有区块的最大区块高度
     *
     * @return
     */
    long getMaxHeight();

    /**
     * 更新最大区块高度
     *         Math.max(currentMaxHeight, maxHeight)
     *
     * @param maxHeight
     */
    void updateMaxHeight(long maxHeight);

    /**
     * 获取指定事件
     *
     * @param key
     * @param sequence
     * @return
     *     if not exist return null
     */
    Event getEvent(String key, long sequence);

    /**
     * 获取指定事件目前最大sequence
     *
     * @param key
     * @return
     */
    long getMaxSequence(String key);

    /**
     * 获取某个key的最大区块高度，注意区别 ${@link EventCache#getMaxHeight()}
     *
     * @param key
     * @return
     */
    long getMaxHeight(String key);

    /**
     * 添加事件
     *
     * @param key
     * @param events
     */
    void addEvents(String key, Event... events);
}
