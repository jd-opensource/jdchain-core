package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.ledger.IllegalAccountStateException;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.UserCAUpdateOperation;
import com.jd.blockchain.ledger.UserDoesNotExistException;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.SecurityContext;
import com.jd.blockchain.ledger.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import com.jd.blockchain.ledger.core.UserAccount;
import com.jd.blockchain.ledger.core.UserAccountSetEditor;
import com.jd.blockchain.ledger.core.UserAccountSetEditorSimple;

import java.security.cert.X509Certificate;
import java.util.Arrays;

public class UserCAUpdateOperationHandle extends AbstractLedgerOperationHandle<UserCAUpdateOperation> {

    public UserCAUpdateOperationHandle() {
        super(UserCAUpdateOperation.class);
    }

    @Override
    protected void doProcess(UserCAUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.UPDATE_USER_CA, MultiIDsPolicy.AT_LEAST_ONE);

        UserAccount user = transactionContext.getDataset().getUserAccountSet().getAccount(op.getUserAddress());
        if (null == user) {
            throw new UserDoesNotExistException(String.format("User doesn't exist! --[Address=%s]", op.getUserAddress()));
        }
        if (user.getState() == AccountState.REVOKE) {
            throw new IllegalAccountStateException(String.format("Can not change user in REVOKE state! --[Address=%s]", op.getUserAddress()));
        }
        // 证书校验
        X509Certificate cert = CertificateUtils.parseCertificate(op.getCertificate());
        CertificateUtils.checkCertificateRolesAny(cert, CertificateRole.PEER, CertificateRole.GW, CertificateRole.USER);
        CertificateUtils.checkValidity(cert);
        X509Certificate[] ledgerCAs = CertificateUtils.parseCertificates(transactionContext.getDataset().getAdminDataset().getAdminSettings().getMetadata().getLedgerCertificates());
        X509Certificate[] issuers = CertificateUtils.findIssuers(cert, ledgerCAs);
        Arrays.stream(issuers).forEach(issuer -> CertificateUtils.checkCACertificate(issuer));
        CertificateUtils.checkValidityAny(issuers);

        // 操作账本；
        if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            ((UserAccountSetEditor) (transactionContext.getDataset().getUserAccountSet())).setCertificate(op.getUserAddress(), op.getCertificate());
        } else {
            ((UserAccountSetEditorSimple) (transactionContext.getDataset().getUserAccountSet())).setCertificate(op.getUserAddress(), op.getCertificate());
        }
    }

}
