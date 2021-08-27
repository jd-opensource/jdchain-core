package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.LedgerAdminDataSetEditor;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

import utils.Bytes;

import com.jd.blockchain.ledger.core.EventManager;


public class ParticipantStateUpdateOperationHandle extends AbstractLedgerOperationHandle<ParticipantStateUpdateOperation> {
    public ParticipantStateUpdateOperationHandle() {
        super(ParticipantStateUpdateOperation.class);
    }

    @Override
    protected void doProcess(ParticipantStateUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
                             OperationHandleContext handleContext, EventManager manager) {

        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_PARTICIPANT, MultiIDsPolicy.AT_LEAST_ONE);

        LedgerAdminDataSetEditor adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

        ParticipantNode[] participants = adminAccountDataSet.getParticipants();

        ParticipantNode participantNode = null;

        for(int i = 0; i < participants.length; i++) {
            if (op.getParticipantID().getPubKey().equals(participants[i].getPubKey())) {
               participantNode = new PartNode(participants[i].getId(), participants[i].getName(), participants[i].getPubKey(), op.getState());
               break;
            }
        }

        // 激活新参与方的共识状态
        adminAccountDataSet.updateParticipant(participantNode);

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

        @Override
        public String getCertificate() {
            throw new IllegalStateException("Not implement!");
        }
    }

}
