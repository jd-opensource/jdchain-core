package com.jd.blockchain.ledger.core;

import com.jd.blockchain.utils.Transactional;

public class LedgerDataSetEditor implements LedgerDataSet, Transactional {

	private LedgerAdminDataSetEditor adminDataset;

	private UserAccountSetEditor userAccountSet;

	private DataAccountSetEditor dataAccountSet;

	private ContractAccountSetEditor contractAccountSet;

	private boolean readonly;

	/**
	 * Create new block;
	 * 
	 * @param adminAccount
	 * @param userAccountSet
	 * @param dataAccountSet
	 * @param contractAccountSet
	 * @param readonly
	 */
	public LedgerDataSetEditor(LedgerAdminDataSetEditor adminAccount, UserAccountSetEditor userAccountSet,
			DataAccountSetEditor dataAccountSet, ContractAccountSetEditor contractAccountSet, boolean readonly) {
		this.adminDataset = adminAccount;
		this.userAccountSet = userAccountSet;
		this.dataAccountSet = dataAccountSet;
		this.contractAccountSet = contractAccountSet;

		this.readonly = readonly;
	}

	@Override
	public LedgerAdminDataSetEditor getAdminDataset() {
		return adminDataset;
	}

	@Override
	public UserAccountSetEditor getUserAccountSet() {
		return userAccountSet;
	}

	@Override
	public DataAccountSetEditor getDataAccountSet() {
		return dataAccountSet;
	}

	@Override
	public ContractAccountSetEditor getContractAccountSet() {
		return contractAccountSet;
	}

	@Override
	public boolean isUpdated() {
		return adminDataset.isUpdated() || userAccountSet.isUpdated() || dataAccountSet.isUpdated()
				|| contractAccountSet.isUpdated();
	}

	@Override
	public void commit() {
		if (readonly) {
			throw new IllegalStateException("Readonly ledger dataset which cann't been committed!");
		}
		if (!isUpdated()) {
			return;
		}

		adminDataset.commit();
		userAccountSet.commit();
		dataAccountSet.commit();
		contractAccountSet.commit();
	}

	@Override
	public void cancel() {
		adminDataset.cancel();
		userAccountSet.cancel();
		dataAccountSet.cancel();
		contractAccountSet.cancel();
	}

	public boolean isReadonly() {
		return readonly;
	}

//	void setReadonly() {
//		this.readonly = true;
//		this.adminDataset.setReadonly();
//		this.userAccountSet.setReadonly();
//		this.dataAccountSet.setReadonly();
//		this.contractAccountSet.setReadonly();
//	}

}