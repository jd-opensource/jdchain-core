package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.ledger.IdentityMode;
import com.jd.blockchain.ledger.IllegalTransactionException;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.RootCAUpdateOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerAdminDataSetEditor;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

import java.security.cert.X509Certificate;

public class RootCAUpdateOperationHandle extends AbstractLedgerOperationHandle<RootCAUpdateOperation> {

    public RootCAUpdateOperationHandle() {
        super(RootCAUpdateOperation.class);
    }

    @Override
    protected void doProcess(RootCAUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.UPDATE_ROOT_CA, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSetEditor adminDataset = transactionContext.getDataset().getAdminDataset();
        if (adminDataset.getMetadata().getIdentityMode() == IdentityMode.CA) {
            X509Certificate certificate = X509Utils.resolveCertificate(op.getCertificate());
            String[] ledgerCAs = adminDataset.getMetadata().getLedgerCAs();
            boolean updated = false;
            for (int i = 0; i < ledgerCAs.length; i++) {
                if (certificate.getPublicKey().equals(X509Utils.resolveCertificate(ledgerCAs[i]).getPublicKey())) {
                    ledgerCAs[i] = op.getCertificate();
                    updated = true;
                    break;
                }
            }
            if (updated) {
                adminDataset.updateLedgerCA(ledgerCAs);
            } else {
                throw new IllegalTransactionException("No ledger ca found!");
            }
        } else {
            throw new IllegalTransactionException("Not in ca mode!");
        }
    }

}
