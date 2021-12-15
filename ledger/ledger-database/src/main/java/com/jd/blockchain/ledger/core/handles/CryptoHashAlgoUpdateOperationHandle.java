package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.crypto.service.sm.SMAlgorithm;
import com.jd.blockchain.ledger.CryptoHashAlgoUpdateOperation;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.core.CryptoConfig;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerAdminDataSet;
import com.jd.blockchain.ledger.core.LedgerAdminDataSetEditor;
import com.jd.blockchain.ledger.core.LedgerAdminDataSetEditorSimple;
import com.jd.blockchain.ledger.core.LedgerConfiguration;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

/**
 * @Author: zhangshuang
 * @Date: 2021/12/7 5:36 PM
 * Version 1.0
 */
public class CryptoHashAlgoUpdateOperationHandle extends AbstractLedgerOperationHandle<CryptoHashAlgoUpdateOperation> {

    public CryptoHashAlgoUpdateOperationHandle() {
        super(CryptoHashAlgoUpdateOperation.class);
    }

    @Override
    protected void doProcess(CryptoHashAlgoUpdateOperation op, LedgerTransactionContext transactionContext, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.SET_CONSENSUS, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSet adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

        String provider = adminAccountDataSet.getAdminSettings().getSettings().getConsensusProvider();

        LedgerSettings origledgerSetting;

        if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            origledgerSetting = ((LedgerAdminDataSetEditor)adminAccountDataSet).getPreviousSetting();
        } else {
            origledgerSetting = ((LedgerAdminDataSetEditorSimple)adminAccountDataSet).getPreviousSetting();
        }

        // update crypto setting
        CryptoAlgorithm newCryptoAlgorithm = null;
        if (op.getHashAlgoName().toUpperCase().equals("SHA256")) {
            newCryptoAlgorithm = ClassicAlgorithm.SHA256;
        } else if (op.getHashAlgoName().toUpperCase().equals("RIPEMD160")) {
            newCryptoAlgorithm = ClassicAlgorithm.RIPEMD160;
        } else if (op.getHashAlgoName().toUpperCase().equals("SM3")) {
            newCryptoAlgorithm = SMAlgorithm.SM3;
        }

        if (newCryptoAlgorithm.name().equals(origledgerSetting.getCryptoSetting().getHashAlgorithm())) {
            return;
        }
        CryptoConfig cryptoConfig = new CryptoConfig();
        cryptoConfig.setSupportedProviders(origledgerSetting.getCryptoSetting().getSupportedProviders());
        cryptoConfig.setAutoVerifyHash(origledgerSetting.getCryptoSetting().getAutoVerifyHash());
        cryptoConfig.setHashAlgorithm(newCryptoAlgorithm);

        // update ledger setting
        LedgerSettings newLedgerSetting = new LedgerConfiguration(provider, adminAccountDataSet.getAdminSettings().getSettings().getConsensusSetting(), cryptoConfig);

        if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            ((LedgerAdminDataSetEditor)adminAccountDataSet).setLedgerSetting(newLedgerSetting);
        } else {
            ((LedgerAdminDataSetEditorSimple)adminAccountDataSet).setLedgerSetting(newLedgerSetting);
        }
    }
}

