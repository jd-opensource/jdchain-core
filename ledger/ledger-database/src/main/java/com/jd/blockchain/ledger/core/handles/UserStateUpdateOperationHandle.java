package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.IllegalTransactionException;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.UserState;
import com.jd.blockchain.ledger.UserStateUpdateOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import com.jd.blockchain.ledger.core.UserAccount;

public class UserStateUpdateOperationHandle extends AbstractLedgerOperationHandle<UserStateUpdateOperation> {

    public UserStateUpdateOperationHandle() {
        super(UserStateUpdateOperation.class);
    }

    @Override
    protected void doProcess(UserStateUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.UPDATE_USER_STATE, MultiIDsPolicy.AT_LEAST_ONE);

        UserAccount user = transactionContext.getDataset().getUserAccountSet().getAccount(op.getUserAddress());
        // REVOKE 状态不可再恢复
        if (user.getState() == UserState.REVOKE) {
            throw new IllegalTransactionException("Can not change user[" + op.getUserAddress() + "] in REVOKE state.");
        }

        // 操作账本；
        transactionContext.getDataset().getUserAccountSet().setState(op.getUserAddress(), op.getState());
    }

}
