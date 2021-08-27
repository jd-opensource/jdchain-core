package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.UserRevokeOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

public class UserRevokeOperationHandle extends AbstractLedgerOperationHandle<UserRevokeOperation> {

    public UserRevokeOperationHandle() {
        super(UserRevokeOperation.class);
    }

    @Override
    protected void doProcess(UserRevokeOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REVOKE_USER, MultiIDsPolicy.AT_LEAST_ONE);

        // 操作账本；
        transactionContext.getDataset().getUserAccountSet().revoke(op.getUserAddress());
    }

}
