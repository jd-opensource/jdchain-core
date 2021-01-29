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

        LedgerAdminDataSetEditor adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

        ConsensusProvider provider = ConsensusProviders.getProvider(adminAccountDataSet.getSettings().getConsensusProvider());

        ConsensusViewSettings consensusSettings = provider.getSettingsFactory().getConsensusSettingsEncoder().decode(adminAccountDataSet.getSettings().getConsensusSetting().toBytes());

        //update consensus settings according to properties config
        ConsensusViewSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().updateSettings(consensusSettings, PropertiesUtils.createProperties(op.getProperties()));

        LedgerSettings ledgerSetting = new LedgerConfiguration(adminAccountDataSet.getSettings().getConsensusProvider(),
                new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)), adminAccountDataSet.getPreviousSetting().getCryptoSetting());

        // set new consensus settings to ledger
        adminAccountDataSet.setLedgerSetting(ledgerSetting);
    }
}
