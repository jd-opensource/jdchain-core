package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;

import utils.Bytes;
import utils.PropertiesUtils;


public class ConsensusSettingsUpdateOperationHandle extends AbstractLedgerOperationHandle<ConsensusSettingsUpdateOperation> {
    public ConsensusSettingsUpdateOperationHandle() {
        super(ConsensusSettingsUpdateOperation.class);
    }

    @Override
    protected void doProcess(ConsensusSettingsUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
                             OperationHandleContext handleContext, EventManager manager) {

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_PARTICIPANT, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSet adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

        ConsensusProvider provider = null;
        ConsensusViewSettings consensusSettings = null;

        if (previousBlockDataset.getAnchorType().equals("default")) {
            provider = ConsensusProviders.getProvider(((LedgerAdminDataSetEditor)adminAccountDataSet).getSettings().getConsensusProvider());
            consensusSettings = provider.getSettingsFactory().getConsensusSettingsEncoder().decode(((LedgerAdminDataSetEditor)adminAccountDataSet).getSettings().getConsensusSetting().toBytes());
        } else {
            provider = ConsensusProviders.getProvider(((LedgerAdminDataSetEditorSimple)adminAccountDataSet).getSettings().getConsensusProvider());
            consensusSettings = provider.getSettingsFactory().getConsensusSettingsEncoder().decode(((LedgerAdminDataSetEditorSimple)adminAccountDataSet).getSettings().getConsensusSetting().toBytes());
        }
        //update consensus settings according to properties config
        ConsensusViewSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().updateSettings(consensusSettings, PropertiesUtils.createProperties(op.getProperties()));

        if (previousBlockDataset.getAnchorType().equals("default")) {
            LedgerSettings ledgerSetting = new LedgerConfiguration(((LedgerAdminDataSetEditor)adminAccountDataSet).getSettings().getConsensusProvider(),
                    new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)), ((LedgerAdminDataSetEditor)adminAccountDataSet).getPreviousSetting().getCryptoSetting());

            // set new consensus settings to ledger
            ((LedgerAdminDataSetEditor)adminAccountDataSet).setLedgerSetting(ledgerSetting);
        } else {
            LedgerSettings ledgerSetting = new LedgerConfiguration(((LedgerAdminDataSetEditorSimple)adminAccountDataSet).getSettings().getConsensusProvider(),
                    new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)), ((LedgerAdminDataSetEditorSimple)adminAccountDataSet).getPreviousSetting().getCryptoSetting());

            // set new consensus settings to ledger
            ((LedgerAdminDataSetEditorSimple)adminAccountDataSet).setLedgerSetting(ledgerSetting);
        }
    }
}
