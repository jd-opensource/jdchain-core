package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;

public class EventPublishOperationHandle extends AbstractLedgerOperationHandle<EventPublishOperation> {

    public EventPublishOperationHandle() {
        super(EventPublishOperation.class);
    }

    @Override
    protected void doProcess(EventPublishOperation op, LedgerTransactionContext transactionContext, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.WRITE_EVENT_ACCOUNT, MultiIDsPolicy.AT_LEAST_ONE);

        EventAccount account = manager.getAccount(op.getEventAddress());
        if (null == account) {
            throw new EventAccountDoesNotExistException(String.format("Event account doesn't exist! --[Address=%s]", op.getEventAddress()));
        }

        // 事件账户写权限控制
        securityPolicy.checkDataPermission(account.getPermission(), DataPermissionType.WRITE);

        // 操作事件账本；
        manager.publish(op.getEventAddress(), op.getEvents());
    }
}
