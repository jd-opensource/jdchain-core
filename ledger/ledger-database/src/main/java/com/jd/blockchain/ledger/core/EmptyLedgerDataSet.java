package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerAdminSettings;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.EmptySkippingIterator;
import com.jd.blockchain.utils.SkippingIterator;

/**
 * 一个只读的空的账本数据集；
 * 
 * @author huanghaiquan
 *
 */
public class EmptyLedgerDataSet implements LedgerDataSet {
	
	public static final LedgerDataSet INSTANCE = new EmptyLedgerDataSet();

	private static final LedgerAdminDataSet EMPTY_ADMIN_DATA = new EmptyAdminData();

	private static final UserAccountSet EMPTY_USER_ACCOUNTS = new EmptyUserAccountSet();

	private static final DataAccountSet EMPTY_DATA_ACCOUNTS = new EmptyDataAccountSet();

	private static final ContractAccountSet EMPTY_CONTRACT_ACCOUNTS = new EmptyContractAccountSet();

	private static final ParticipantCollection EMPTY_PARTICIPANTS = new EmptyParticipantData();
	
	private EmptyLedgerDataSet() {
	}

	@Override
	public LedgerAdminDataSet getAdminDataset() {
		return EMPTY_ADMIN_DATA;
	}

	@Override
	public UserAccountSet getUserAccountSet() {
		return EMPTY_USER_ACCOUNTS;
	}

	@Override
	public DataAccountSet getDataAccountSet() {
		return EMPTY_DATA_ACCOUNTS;
	}

	@Override
	public ContractAccountSet getContractAccountSet() {
		return EMPTY_CONTRACT_ACCOUNTS;
	}

	private static class EmptyAdminData implements LedgerAdminDataSet {

		@Override
		public LedgerAdminSettings getAdminSettings() {
			return null;
		}

		@Override
		public ParticipantCollection getParticipantDataset() {
			return EMPTY_PARTICIPANTS;
		}

	}

	private static class EmptyParticipantData implements ParticipantCollection {

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

	private static class EmptyUserAccountSet extends EmptyAccountSet<UserAccount> implements UserAccountSet {

	}

	private static class EmptyDataAccountSet extends EmptyAccountSet<DataAccount> implements DataAccountSet {

	}

	private static class EmptyContractAccountSet extends EmptyAccountSet<ContractAccount>
			implements ContractAccountSet {

	}

}
