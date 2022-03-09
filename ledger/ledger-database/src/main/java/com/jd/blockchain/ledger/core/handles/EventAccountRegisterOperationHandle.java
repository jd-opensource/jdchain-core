package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import utils.Bytes;

public class EventAccountRegisterOperationHandle extends AbstractLedgerOperationHandle<EventAccountRegisterOperation> {

    public EventAccountRegisterOperationHandle() {
        super(EventAccountRegisterOperation.class);
    }

    @Override
    protected void doProcess(EventAccountRegisterOperation op, LedgerTransactionContext transactionContext, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
// TODO: 请求者应该提供数据账户的公钥签名，以更好地确保注册人对该地址和公钥具有合法使用权；

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_EVENT_ACCOUNT, MultiIDsPolicy.AT_LEAST_ONE);

        // 操作事件账本；
        PermissionAccount account = manager.registerAccount(op.getEventAccountID());
        account.setPermission(new AccountDataPermission(AccountType.EVENT, requestContext.getEndpointAddresses().toArray(new Bytes[0])));
    }
}
