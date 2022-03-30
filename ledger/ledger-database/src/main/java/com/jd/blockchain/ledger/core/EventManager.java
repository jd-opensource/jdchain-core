package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.DataVersionConflictException;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.EventPublishOperation;

import com.jd.blockchain.ledger.LedgerDataStructure;
import utils.Bytes;

/**
 * 事件管理器
 * 处理事件账户注册，用户事件发布，系统事件发布，事件监听
 */
public class EventManager implements EventOperationHandle {

    private TransactionRequestExtension request;
    private LedgerTransactionContext txCtx;
    private LedgerQuery ledger;

    public EventManager(TransactionRequestExtension request, LedgerTransactionContext txCtx, LedgerQuery ledger) {
        this.request = request;
        this.txCtx = txCtx;
        this.ledger = ledger;

    }

    @Override
    public EventAccount registerAccount(BlockchainIdentity identity) {
        return ((EventAccountSetEditor) (txCtx.getEventSet().getEventAccountSet())).register(identity.getAddress(), identity.getPubKey(), null);
    }

    public EventAccount getAccount(Bytes address) {
        return ((EventAccountSetEditor) (txCtx.getEventSet().getEventAccountSet())).getAccount(address);
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
        long v = 0;

        v = ((MerkleEventGroupPublisher)(txCtx.getEventSet().getSystemEventGroup())).publish(new EventInfo(eventName, latestSequence+1, content, request.getTransactionHash(), txCtx.getBlockHeight()));

        if (v < 0) {
            throw new DataVersionConflictException();
        }

        return v;
    }

}
