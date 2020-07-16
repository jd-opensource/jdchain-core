package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.transaction.UserRegisterOpTemplate;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.ledger.core.EventManager;

import java.util.Properties;

public class ParticipantRegisterOperationHandle extends AbstractLedgerOperationHandle<ParticipantRegisterOperation> {
    public ParticipantRegisterOperationHandle() {
        super(ParticipantRegisterOperation.class);
    }

    @Override
    protected void doProcess(ParticipantRegisterOperation op, LedgerDataset newBlockDataset,
                             TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
                             OperationHandleContext handleContext, EventManager manager) {

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_PARTICIPANT, MultiIDsPolicy.AT_LEAST_ONE);

        ParticipantRegisterOperation participantRegOp = (ParticipantRegisterOperation) op;

        LedgerAdminDataset adminAccountDataSet = newBlockDataset.getAdminDataset();

        ConsensusProvider provider = ConsensusProviders.getProvider(adminAccountDataSet.getSettings().getConsensusProvider());

        ParticipantNode participantNode = new PartNode((int)(adminAccountDataSet.getParticipantCount()), op.getParticipantName(), op.getParticipantRegisterIdentity().getPubKey(), ParticipantNodeState.READY);

        //add new participant as consensus node
        adminAccountDataSet.addParticipant(participantNode);

        ConsensusSettings consensusSettings = provider.getSettingsFactory().getConsensusSettingsEncoder().decode(adminAccountDataSet.getSettings().getConsensusSetting().toBytes());

        ConsensusSettings newConsensusSettings = provider.getSettingsFactory().getConsensusSettingsBuilder().writeSettings(consensusSettings, participantRegOp.getConsensusSettings(), ParticipantNodeOp.REGIST);

        //update consensus nodes setting, add new participant for ledger setting
        LedgerSettings ledgerSetting = new LedgerConfiguration(adminAccountDataSet.getSettings().getConsensusProvider(),
                new Bytes(provider.getSettingsFactory().getConsensusSettingsEncoder().encode(newConsensusSettings)), adminAccountDataSet.getPreviousSetting().getCryptoSetting());

        adminAccountDataSet.setLedgerSetting(ledgerSetting);

        // Build UserRegisterOperation, reg participant as user
        UserRegisterOperation userRegOp = new UserRegisterOpTemplate(participantRegOp.getParticipantRegisterIdentity());
        handleContext.handle(userRegOp);
    }

    private static class PartNode implements ParticipantNode {

        private int id;

        private Bytes address;

        private String name;

        private PubKey pubKey;

        private ParticipantNodeState participantNodeState;

        public PartNode(int id, String name, PubKey pubKey, ParticipantNodeState participantNodeState) {
            this.id = id;
            this.name = name;
            this.pubKey = pubKey;
            this.address = AddressEncoding.generateAddress(pubKey);
            this.participantNodeState = participantNodeState;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public Bytes getAddress() {
            return address;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public PubKey getPubKey() {
            return pubKey;
        }

        @Override
        public ParticipantNodeState getParticipantNodeState() {
            return participantNodeState;
        }
    }


}
