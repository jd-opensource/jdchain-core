package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.core.*;

public class EventPublishOperationHandle extends AbstractLedgerOperationHandle<EventPublishOperation> {

    public EventPublishOperationHandle() {
        super(EventPublishOperation.class);
    }

    @Override
    protected void doProcess(EventPublishOperation op, LedgerDataSetEditor newBlockDataset, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.WRITE_EVENT_ACCOUNT, MultiIDsPolicy.AT_LEAST_ONE);

        // 操作事件账本；
        manager.publish(op.getEventAddress(), op.getEvents());
    }
}
