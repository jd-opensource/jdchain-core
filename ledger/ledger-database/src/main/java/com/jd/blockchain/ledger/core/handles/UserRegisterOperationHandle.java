package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ca.CertificateType;
import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.IdentityMode;
import com.jd.blockchain.ledger.IllegalTransactionException;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import utils.Bytes;
import utils.StringUtils;

import java.security.cert.X509Certificate;

public class UserRegisterOperationHandle extends AbstractLedgerOperationHandle<UserRegisterOperation> {

    public UserRegisterOperationHandle() {
        super(UserRegisterOperation.class);
    }

    @Override
    protected void doProcess(UserRegisterOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_USER, MultiIDsPolicy.AT_LEAST_ONE);

        // 证书模式下必须传递证书
        if (transactionContext.getDataset().getAdminDataset().getMetadata().getIdentityMode() == IdentityMode.CA) {
            if (StringUtils.isEmpty(op.getCertificate())) {
                throw new IllegalTransactionException("User ca is empty!");
            }

            X509Certificate cert = X509Utils.resolveCertificate(op.getCertificate());
            X509Utils.checkCertificateTypesAny(cert, CertificateType.PEER, CertificateType.GW, CertificateType.USER);
            X509Utils.checkValidity(cert);
            X509Certificate[] ledgerCAs = X509Utils.resolveCertificates(transactionContext.getDataset().getAdminDataset().getMetadata().getLedgerCAs());
            X509Certificate[] issuers = X509Utils.findIssuers(cert, ledgerCAs);
            X509Utils.checkCertificateType(issuers, CertificateType.LEDGER);
            X509Utils.checkValidityAny(issuers);
        }

        // 操作账本；
        BlockchainIdentity bid = op.getUserID();
        Bytes userAddress = op.getUserID().getAddress();
        transactionContext.getDataset().getUserAccountSet().register(userAddress, bid.getPubKey(), op.getCertificate());
    }

}
