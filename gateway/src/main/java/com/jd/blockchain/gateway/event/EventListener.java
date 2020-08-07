package com.jd.blockchain.gateway.event;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.sdk.EventPoint;

/**
 * 事件监听器
 *
 * @author shaozhuguang
 *
 */
public interface EventListener {

    /**
     * 启动监听器
     *
     */
    void start();

    /**
     * 获取事件
     *
     * @param ledgerHash
     * @param eventPoint
     * @param fromSequence
     * @param maxCount
     * @return
     */
    Event[] getEvents(HashDigest ledgerHash, EventPoint eventPoint, long fromSequence, int maxCount);

    /**
     * 获取系统事件
     *
     * @param ledgerHash
     * @param eventName
     * @param fromSequence
     * @param maxCount
     * @return
     */
    Event[] getSystemEvents(HashDigest ledgerHash, String eventName, long fromSequence, int maxCount);

    /**
     * 返回自定义事件；
     *
     * @param ledgerHash   账本哈希；
     * @param address      事件账户地址；
     * @param eventName    事件名；
     * @param fromSequence 开始的事件序列号；
     * @param maxCount     最大数量；
     * @return
     */
    Event[] getUserEvents(HashDigest ledgerHash, String address, String eventName, long fromSequence, int maxCount);

}
