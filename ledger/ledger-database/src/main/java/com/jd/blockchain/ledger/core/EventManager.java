package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.DataVersionConflictException;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;

/**
 * 事件管理器
 * 处理事件账户注册，用户事件发布，系统事件发布，事件监听
 */
public class EventManager implements EventOperationHandle, Transactional {

    private LedgerTransactionContext txCtx;

    public EventManager(LedgerTransactionContext txCtx) {
        this.txCtx = txCtx;
    }

    @Override
    public void registerAccount(BlockchainIdentity identity) {
        txCtx.getEventSet().getUserEvents().register(identity.getAddress(), identity.getPubKey(), null);
    }

    @Override
    public void publish(Bytes address, EventPublishOperation.EventEntry[] events) {
        EventPublishingAccount account = txCtx.getEventSet().getUserEvents().getAccount(address);
        for (EventPublishOperation.EventEntry event : events) {
            long v = account.publish(new EventInfo(event.getName(), event.getSequence()+1, event.getContent(), txCtx.getTransactionSet().getRootHash(), txCtx.getBlockHeight()));
            if (v < 0) {
                throw new DataVersionConflictException();
            }
        }
    }

    @Override
    public long publish(String eventName, BytesValue content, long latestSequence) {
        long v = txCtx.getEventSet().getSystemEvents().publish(new EventInfo(eventName, latestSequence+1, content, txCtx.getTransactionSet().getRootHash(), txCtx.getBlockHeight()));
        if (v < 0) {
            throw new DataVersionConflictException();
        }

        return v;
    }

    @Override
    public boolean isUpdated() {
        return txCtx.getEventSet().isUpdated();
    }

    @Override
    public void commit() {
        txCtx.getEventSet().commit();
    }

    @Override
    public void cancel() {
        txCtx.getEventSet().cancel();
    }
}
