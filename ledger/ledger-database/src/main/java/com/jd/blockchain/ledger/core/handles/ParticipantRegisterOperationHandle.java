package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.transaction.UserRegisterOpTemplate;

import utils.Bytes;

import com.jd.blockchain.ledger.core.EventManager;

public class ParticipantRegisterOperationHandle extends AbstractLedgerOperationHandle<ParticipantRegisterOperation> {
	public ParticipantRegisterOperationHandle() {
		super(ParticipantRegisterOperation.class);
	}

	@Override
	protected void doProcess(ParticipantRegisterOperation op, LedgerTransactionContext transactionContext,
			TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
			OperationHandleContext handleContext, EventManager manager) {

		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_PARTICIPANT, MultiIDsPolicy.AT_LEAST_ONE);

		ParticipantRegisterOperation participantRegOp = (ParticipantRegisterOperation) op;

		LedgerAdminDataSetEditor adminAccountDataSet = transactionContext.getDataset().getAdminDataset();

		ParticipantNode participantNode = new PartNode((int) (adminAccountDataSet.getParticipantCount()),
				participantRegOp.getParticipantName(), participantRegOp.getParticipantID().getPubKey(),
				ParticipantNodeState.READY);

		// add new participant
		adminAccountDataSet.addParticipant(participantNode);

		// Build UserRegisterOperation, reg participant as user
		UserRegisterOperation userRegOp = new UserRegisterOpTemplate(participantRegOp.getParticipantID());
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
