package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.IdentityMode;
import com.jd.blockchain.ledger.IllegalTransactionException;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.SecurityContext;
import com.jd.blockchain.ledger.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import com.jd.blockchain.ledger.core.UserAccountSetEditor;
import com.jd.blockchain.ledger.core.UserAccountSetEditorSimple;
import utils.Bytes;
import utils.StringUtils;

import java.security.cert.X509Certificate;
import java.util.Arrays;

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
        if (transactionContext.getDataset().getAdminDataset().getAdminSettings().getMetadata().getIdentityMode() == IdentityMode.CA) {
            if (StringUtils.isEmpty(op.getCertificate())) {
                throw new IllegalTransactionException("User certificate is empty!");
            }

            X509Certificate cert = CertificateUtils.parseCertificate(op.getCertificate());
            CertificateUtils.checkCertificateRolesAny(cert, CertificateRole.PEER, CertificateRole.GW, CertificateRole.USER);
            CertificateUtils.checkValidity(cert);
            X509Certificate[] ledgerCAs = CertificateUtils.parseCertificates(transactionContext.getDataset().getAdminDataset().getAdminSettings().getMetadata().getLedgerCertificates());
            X509Certificate[] issuers = CertificateUtils.findIssuers(cert, ledgerCAs);
            Arrays.stream(issuers).forEach(issuer -> CertificateUtils.checkCACertificate(issuer));
            CertificateUtils.checkValidityAny(issuers);
        }

        // 操作账本；
        BlockchainIdentity bid = op.getUserID();

		Bytes userAddress = bid.getAddress();

		if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
			((UserAccountSetEditor)(transactionContext.getDataset().getUserAccountSet())).register(userAddress, bid.getPubKey(), op.getCertificate());
		} else {
			((UserAccountSetEditorSimple)(transactionContext.getDataset().getUserAccountSet())).register(userAddress, bid.getPubKey(), op.getCertificate());
		}

	}

}
