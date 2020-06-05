package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.utils.Bytes;

/**
 * 事件交易处理
 */
public interface EventOperationHandle {

    /**
     * 注册事件账户
     *
     * @param identity
     * @return
     */
    void registerAccount(BlockchainIdentity identity);

    /**
     * 发布用户自定义事件
     *
     * @param address
     * @param events
     * @return
     */
    void publish(Bytes address, EventPublishOperation.EventEntry[] events);

}
