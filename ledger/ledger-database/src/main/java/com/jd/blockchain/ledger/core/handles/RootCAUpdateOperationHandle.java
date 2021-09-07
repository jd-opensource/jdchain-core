package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.ledger.IdentityMode;
import com.jd.blockchain.ledger.LedgerException;
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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

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
            String[] ledgerCAs = adminDataset.getMetadata().getLedgerCertificates();
            Map<PublicKey, String> ledgerCAMap = new HashMap<>();
            for (int i = 0; i < ledgerCAs.length; i++) {
                X509Certificate cert = X509Utils.resolveCertificate(ledgerCAs[i]);
                PublicKey publicKey = cert.getPublicKey();
                ledgerCAMap.put(publicKey, ledgerCAs[i]);
            }
            String[] certificatesAdd = op.getCertificatesAdd();
            for (String cert : certificatesAdd) {
                X509Certificate certificate = X509Utils.resolveCertificate(cert);
                X509Utils.checkCertificateRolesAny(certificate, CertificateRole.ROOT, CertificateRole.CA);
                X509Utils.checkValidity(certificate);
                if (!ledgerCAMap.containsKey(certificate.getPublicKey())) {
                    ledgerCAMap.put(certificate.getPublicKey(), cert);
                } else {
                    throw new LedgerException("Certificate [" + X509Utils.toPEMString(certificate) + "] already exists in the ledger!");
                }
            }
            String[] certificatesUpdate = op.getCertificatesUpdate();
            for (String cert : certificatesUpdate) {
                X509Certificate certificate = X509Utils.resolveCertificate(cert);
                X509Utils.checkCertificateRolesAny(certificate, CertificateRole.ROOT, CertificateRole.CA);
                X509Utils.checkValidity(certificate);
                if (ledgerCAMap.containsKey(certificate.getPublicKey())) {
                    ledgerCAMap.put(certificate.getPublicKey(), cert);
                } else {
                    throw new LedgerException("Certificate [" + X509Utils.toPEMString(certificate) + "] not exists in the ledger!");
                }
            }
            String[] certificatesRemove = op.getCertificatesRemove();
            for (String cert : certificatesRemove) {
                X509Certificate certificate = X509Utils.resolveCertificate(cert);
                X509Utils.checkCertificateRolesAny(certificate, CertificateRole.ROOT, CertificateRole.CA);
                if (ledgerCAMap.containsKey(certificate.getPublicKey())) {
                    ledgerCAMap.remove(certificate.getPublicKey(), cert);
                } else {
                    throw new LedgerException("Certificate [" + X509Utils.toPEMString(certificate) + "] not exists in the ledger!");
                }
            }

            adminDataset.updateLedgerCA(ledgerCAMap.values().toArray(new String[0]));
        } else {
            throw new LedgerException("Not in CA identity mode!");
        }
    }

}
