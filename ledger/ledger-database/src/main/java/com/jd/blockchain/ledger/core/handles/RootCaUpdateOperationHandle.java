package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.RootCaUpdateOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

public class RootCaUpdateOperationHandle extends AbstractLedgerOperationHandle<RootCaUpdateOperation> {

    public RootCaUpdateOperationHandle() {
        super(RootCaUpdateOperation.class);
    }

    @Override
    protected void doProcess(RootCaUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.UPDATE_ROOT_CA, MultiIDsPolicy.AT_LEAST_ONE);

        // 操作账本；
        transactionContext.getDataset().getAdminDataset().updateCa(op.getCertificate());
    }

}
