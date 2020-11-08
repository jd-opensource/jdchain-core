package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerAdminSettings;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.EmptySkippingIterator;
import com.jd.blockchain.utils.SkippingIterator;

public class EmptyLedgerDataset implements LedgerDataQuery {
	
	private static final LedgerAdminDataQuery EMPTY_ADMIN_DATA = new EmptyAdminData();
	
	private static final UserAccountCollection EMPTY_USER_ACCOUNTS = new EmptyUserAccountSet();
	
	private static final DataAccountCollection EMPTY_DATA_ACCOUNTS = new EmptyDataAccountSet();
	
	private static final ContractAccountCollection EMPTY_CONTRACT_ACCOUNTS = new EmptyContractAccountSet();

	private static final ParticipantCollection EMPTY_PARTICIPANTS = new EmptyParticipantData();

	@Override
	public LedgerAdminDataQuery getAdminDataset() {
		return EMPTY_ADMIN_DATA;
	}

	@Override
	public UserAccountCollection getUserAccountSet() {
		return EMPTY_USER_ACCOUNTS;
	}

	@Override
	public DataAccountCollection getDataAccountSet() {
		return EMPTY_DATA_ACCOUNTS;
	}

	@Override
	public ContractAccountCollection getContractAccountset() {
		return EMPTY_CONTRACT_ACCOUNTS;
	}


	private static class EmptyAdminData implements LedgerAdminDataQuery{
		

		@Override
		public LedgerAdminSettings getAdminSettings() {
			return null;
		}

		@Override
		public ParticipantCollection getParticipantDataset() {
			return EMPTY_PARTICIPANTS;
		}
		
	}
	
	private static class EmptyParticipantData implements ParticipantCollection{

		@Override
		public HashDigest getRootHash() {
			return null;
		}

		@Override
		public MerkleProof getProof(Bytes key) {
			return null;
		}

		@Override
		public long getParticipantCount() {
			return 0;
		}

		@Override
		public boolean contains(Bytes address) {
			return false;
		}

		@Override
		public ParticipantNode getParticipant(Bytes address) {
			return null;
		}

		@Override
		public ParticipantNode[] getParticipants() {
			return null;
		}

		@Override
		public SkippingIterator<ParticipantNode> getAllParticipants() {
			return EmptySkippingIterator.instance();
		}
		
	}
	
	private static class EmptyUserAccountSet extends EmptyAccountSet<UserAccount> implements UserAccountCollection{

	}
	
	private static class EmptyDataAccountSet extends EmptyAccountSet<DataAccount> implements DataAccountCollection{

	}
	
	private static class EmptyContractAccountSet extends EmptyAccountSet<ContractAccount> implements ContractAccountCollection{
		
	}

	
}
