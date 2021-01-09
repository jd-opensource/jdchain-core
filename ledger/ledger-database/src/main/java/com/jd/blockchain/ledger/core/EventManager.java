package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.DataVersionConflictException;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.EventPublishOperation;

import utils.Bytes;

/**
 * 事件管理器
 * 处理事件账户注册，用户事件发布，系统事件发布，事件监听
 */
public class EventManager implements EventOperationHandle {

    private TransactionRequestExtension request;
    private LedgerTransactionContext txCtx;

    public EventManager(TransactionRequestExtension request, LedgerTransactionContext txCtx) {
        this.request = request;
        this.txCtx = txCtx;
    }

    @Override
    public void registerAccount(BlockchainIdentity identity) {
        txCtx.getEventSet().getEventAccountSet().register(identity.getAddress(), identity.getPubKey(), null);
    }

    @Override
    public void publish(Bytes address, EventPublishOperation.EventEntry[] events) {
        EventPublishingAccount account = txCtx.getEventSet().getEventAccountSet().getAccount(address);
        for (EventPublishOperation.EventEntry event : events) {
            long v = account.publish(new EventInfo(address, event.getName(), event.getSequence()+1, event.getContent(), request.getTransactionHash(), txCtx.getBlockHeight()));
            if (v < 0) {
                throw new DataVersionConflictException();
            }
        }
    }

    @Override
    public long publish(String eventName, BytesValue content, long latestSequence) {
        long v = txCtx.getEventSet().getSystemEventGroup().publish(new EventInfo(eventName, latestSequence+1, content, request.getTransactionHash(), txCtx.getBlockHeight()));
        if (v < 0) {
            throw new DataVersionConflictException();
        }

        return v;
    }

}
