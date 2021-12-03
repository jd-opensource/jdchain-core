package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.ReplicaImpl;
import com.jd.blockchain.ledger.ConsensusTypeUpdateOperation;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.ParticipantNode;
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
import utils.Bytes;
import utils.PropertiesUtils;

/**
 * @Author: zhangshuang
 * @Date: 2021/11/30 8:01 PM
 * Version 1.0
 */
public class ConsensusTypeUpdateOperationHandle extends AbstractLedgerOperationHandle<ConsensusTypeUpdateOperation> {
    public ConsensusTypeUpdateOperationHandle() {
        super(ConsensusTypeUpdateOperation.class);
    }


    @Override
    protected void doProcess(ConsensusTypeUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
                             OperationHandleContext handleContext, EventManager manager) {


        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.SET_CONSENSUS, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSet adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

        ConsensusProvider provider = ConsensusProviders.getProvider(op.getProviderName());

        int participantCount = (int) adminAccountDataSet.getParticipantDataset().getParticipantCount();
        ParticipantNode[] participantNodes = adminAccountDataSet.getAdminSettings().getParticipants();

        Replica[] replicas = new Replica[participantCount];
        for (int i = 0; i < participantCount; i++) {
            replicas[i] = new ReplicaImpl(participantNodes[i].getId(), participantNodes[i].getAddress(), participantNodes[i].getName(), participantNodes[i].getPubKey());
        }

        //create new consensus settings according to properties config
        ConsensusViewSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().createSettings(PropertiesUtils.createProperties(op.getProperties()), replicas);

        if (previousBlockDataset.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            LedgerSettings ledgerSetting = new LedgerConfiguration(op.getProviderName(), new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)),
                    ((LedgerAdminDataSetEditor)adminAccountDataSet).getPreviousSetting().getCryptoSetting());

            // set new consensus settings to ledger
            ((LedgerAdminDataSetEditor)adminAccountDataSet).setLedgerSetting(ledgerSetting);
        } else {
            LedgerSettings ledgerSetting = new LedgerConfiguration(op.getProviderName(), new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)),
                    ((LedgerAdminDataSetEditorSimple)adminAccountDataSet).getPreviousSetting().getCryptoSetting());

            // set new consensus settings to ledger
            ((LedgerAdminDataSetEditorSimple)adminAccountDataSet).setLedgerSetting(ledgerSetting);
        }
    }
}
