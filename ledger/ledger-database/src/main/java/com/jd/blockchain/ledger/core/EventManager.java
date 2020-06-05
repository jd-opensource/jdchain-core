package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.DataVersionConflictException;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;

/**
 * 事件管理器
 * 处理事件账户注册，用户事件发布，系统事件发布，事件监听
 */
public class EventManager implements EventOperationHandle, EventPublisher, Transactional {

    private LedgerEventSet eventSet;

    public EventManager(LedgerEventSet eventSet) {
        this.eventSet = eventSet;
    }

    @Override
    public void registerAccount(BlockchainIdentity identity) {
        eventSet.getUserEvents().register(identity.getAddress(), identity.getPubKey(), null);
    }

    @Override
    public void publish(Bytes address, EventPublishOperation.EventEntry[] events) {
        EventPublishingAccount account = eventSet.getUserEvents().getAccount(address);
        for (EventPublishOperation.EventEntry event : events) {
            long v = account.publish(event.getName(), event.getContent(), event.getSequence());
            if (v < 0) {
                throw new DataVersionConflictException();
            }
        }
    }

    @Override
    public long publish(String eventName, BytesValue content, long latestSequence) {
        long v = eventSet.getSystemEvents().publish(eventName, content, latestSequence);
        if (v < 0) {
            throw new DataVersionConflictException();
        }

        return v;
    }

    @Override
    public boolean isUpdated() {
        return eventSet.isUpdated();
    }

    @Override
    public void commit() {
        eventSet.commit();
    }

    @Override
    public void cancel() {
        eventSet.cancel();
    }
}
