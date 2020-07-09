package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.EventAccountRegisterOperation;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.core.*;

public class EventAccountRegisterOperationHandle extends AbstractLedgerOperationHandle<EventAccountRegisterOperation> {

    public EventAccountRegisterOperationHandle() {
        super(EventAccountRegisterOperation.class);
    }

    @Override
    protected void doProcess(EventAccountRegisterOperation op, LedgerDataset newBlockDataset, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
// TODO: 请求者应该提供数据账户的公钥签名，以更好地确保注册人对该地址和公钥具有合法使用权；

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_EVENT_ACCOUNT, MultiIDsPolicy.AT_LEAST_ONE);

        // 操作事件账本；
        manager.registerAccount(op.getEventAccountID());
    }
}
