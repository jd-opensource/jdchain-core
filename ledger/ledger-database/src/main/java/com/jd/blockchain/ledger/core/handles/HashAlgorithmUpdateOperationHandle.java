package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.crypto.service.sm.SMAlgorithm;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;

/**
 * @Author: zhangshuang
 * @Date: 2021/12/7 5:36 PM
 * Version 1.0
 */
public class HashAlgorithmUpdateOperationHandle extends AbstractLedgerOperationHandle<HashAlgorithmUpdateOperation> {

    public HashAlgorithmUpdateOperationHandle() {
        super(HashAlgorithmUpdateOperation.class);
    }

    @Override
    protected void doProcess(HashAlgorithmUpdateOperation op, LedgerTransactionContext transactionContext, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.SET_CRYPTO, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSet adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

        String provider = adminAccountDataSet.getAdminSettings().getSettings().getConsensusProvider();

        LedgerSettings origledgerSetting;

        if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            origledgerSetting = ((LedgerAdminDataSetEditor) adminAccountDataSet).getPreviousSetting();
        } else {
            origledgerSetting = ((LedgerAdminDataSetEditorSimple) adminAccountDataSet).getPreviousSetting();
        }

        String algorithmName = op.getAlgorithm();
        if (algorithmName.equals(origledgerSetting.getCryptoSetting().getHashAlgorithm())) {
            return;
        }
        // update crypto setting
        CryptoAlgorithm newCryptoAlgorithm;
        if (algorithmName.equals(ClassicAlgorithm.SHA256.name())) {
            newCryptoAlgorithm = ClassicAlgorithm.SHA256;
        } else if (algorithmName.equals(ClassicAlgorithm.RIPEMD160.name())) {
            newCryptoAlgorithm = ClassicAlgorithm.RIPEMD160;
        } else if (algorithmName.equals(SMAlgorithm.SM3.name())) {
            newCryptoAlgorithm = SMAlgorithm.SM3;
        } else {
            throw new UnsupportedHashAlgorithmException(String.format("Unsupported hash algorithm: %s", algorithmName));
        }
        CryptoConfig cryptoConfig = new CryptoConfig();
        cryptoConfig.setSupportedProviders(origledgerSetting.getCryptoSetting().getSupportedProviders());
        cryptoConfig.setAutoVerifyHash(origledgerSetting.getCryptoSetting().getAutoVerifyHash());
        cryptoConfig.setHashAlgorithm(newCryptoAlgorithm);

        // update ledger setting
        LedgerSettings newLedgerSetting = new LedgerConfiguration(provider, adminAccountDataSet.getAdminSettings().getSettings().getConsensusSetting(), cryptoConfig);

        if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            ((LedgerAdminDataSetEditor) adminAccountDataSet).setLedgerSetting(newLedgerSetting);
        } else {
            ((LedgerAdminDataSetEditorSimple) adminAccountDataSet).setLedgerSetting(newLedgerSetting);
        }
    }
}

