package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.consensus.*;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import utils.Bytes;
import utils.PropertiesUtils;
import utils.StringUtils;

import java.util.ArrayList;


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
        securityPolicy.checkEndpointPermission(LedgerPermission.SET_CONSENSUS, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSet adminAccountDataSet = transactionContext.getDataset().getAdminDataset();
        String provider = adminAccountDataSet.getAdminSettings().getSettings().getConsensusProvider();
        if (StringUtils.isEmpty(op.getProvider()) || provider.equals(op.getProvider())) {
            updateProperties(op, adminAccountDataSet, previousBlockDataset);
        } else {
            switchConsensus(op, adminAccountDataSet, previousBlockDataset);
        }
    }

    private void updateProperties(ConsensusSettingsUpdateOperation op, LedgerAdminDataSet adminAccountDataSet, LedgerQuery previousBlockDataset) {
        ConsensusProvider provider = null;
        ConsensusViewSettings consensusSettings = null;

        provider = ConsensusProviders.getProvider(((LedgerAdminDataSetEditor) adminAccountDataSet).getSettings().getConsensusProvider());
        consensusSettings = provider.getSettingsFactory().getConsensusSettingsEncoder().decode(((LedgerAdminDataSetEditor) adminAccountDataSet).getSettings().getConsensusSetting().toBytes());

        //update consensus settings according to properties config
        ConsensusViewSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().updateSettings(consensusSettings, PropertiesUtils.createProperties(op.getProperties()));

        LedgerSettings ledgerSetting = new LedgerConfiguration(((LedgerAdminDataSetEditor) adminAccountDataSet).getSettings().getConsensusProvider(),
                new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)), ((LedgerAdminDataSetEditor) adminAccountDataSet).getPreviousSetting().getCryptoSetting());

        // set new consensus settings to ledger
        ((LedgerAdminDataSetEditor) adminAccountDataSet).setLedgerSetting(ledgerSetting);

    }

    private void switchConsensus(ConsensusSettingsUpdateOperation op, LedgerAdminDataSet adminAccountDataSet, LedgerQuery previousBlockDataset) {
        ConsensusProvider provider = ConsensusProviders.getProvider(op.getProvider());

        int participantCount = (int) adminAccountDataSet.getParticipantDataset().getParticipantCount();
        ParticipantNode[] participantNodes = adminAccountDataSet.getAdminSettings().getParticipants();

        ArrayList<Replica> replicas = new ArrayList<>();

        for (int i = 0; i < participantCount; i++) {
            if (participantNodes[i].getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {
                Replica replica = new ReplicaImpl(participantNodes[i].getId(), participantNodes[i].getAddress(), participantNodes[i].getName(), participantNodes[i].getPubKey());
                replicas.add(replica);
            }
        }

        //create new consensus settings according to properties config
        ConsensusViewSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().createSettings(PropertiesUtils.createProperties(op.getProperties()), replicas.toArray(new Replica[replicas.size()]));

        LedgerSettings ledgerSetting = new LedgerConfiguration(op.getProvider(), new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)),
                ((LedgerAdminDataSetEditor) adminAccountDataSet).getPreviousSetting().getCryptoSetting());

        // set new consensus settings to ledger
        ((LedgerAdminDataSetEditor) adminAccountDataSet).setLedgerSetting(ledgerSetting);
    }
}
