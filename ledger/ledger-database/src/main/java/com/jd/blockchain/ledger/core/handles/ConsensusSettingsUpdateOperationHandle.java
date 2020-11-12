package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.PropertiesUtils;


public class ConsensusSettingsUpdateOperationHandle extends AbstractLedgerOperationHandle<ConsensusSettingsUpdateOperation> {
    public ConsensusSettingsUpdateOperationHandle() {
        super(ConsensusSettingsUpdateOperation.class);
    }

    @Override
    protected void doProcess(ConsensusSettingsUpdateOperation op, LedgerDataSetEditor newBlockDataset,
                             TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
                             OperationHandleContext handleContext, EventManager manager) {

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_PARTICIPANT, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSetEditor adminAccountDataSet = newBlockDataset.getAdminDataset();

        ConsensusProvider provider = ConsensusProviders.getProvider(adminAccountDataSet.getSettings().getConsensusProvider());

        ConsensusSettings consensusSettings = provider.getSettingsFactory().getConsensusSettingsEncoder().decode(adminAccountDataSet.getSettings().getConsensusSetting().toBytes());

        //update consensus settings according to properties config
        ConsensusSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().updateSettings(consensusSettings, PropertiesUtils.createProperties(op.getProperties()));

        LedgerSettings ledgerSetting = new LedgerConfiguration(adminAccountDataSet.getSettings().getConsensusProvider(),
                new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)), adminAccountDataSet.getPreviousSetting().getCryptoSetting());

        // set new consensus settings to ledger
        adminAccountDataSet.setLedgerSetting(ledgerSetting);
    }
}
