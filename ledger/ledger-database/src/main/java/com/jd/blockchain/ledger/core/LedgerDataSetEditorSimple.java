package com.jd.blockchain.ledger.core;

import utils.Transactional;

public class LedgerDataSetEditorSimple implements LedgerDataSet, Transactional {

	private LedgerAdminDataSetEditorSimple adminDataset;

	private UserAccountSetEditorSimple userAccountSet;

	private DataAccountSetEditorSimple dataAccountSet;

	private ContractAccountSetEditorSimple contractAccountSet;

	private boolean readonly;

	/**
	 * Create new block;
	 *
	 * @param adminAccountSimple
	 * @param userAccountSetSimple
	 * @param dataAccountSetSimple
	 * @param contractAccountSetSimple
	 * @param readonly
	 */
	public LedgerDataSetEditorSimple(LedgerAdminDataSetEditorSimple adminAccount, UserAccountSetEditorSimple userAccountSet,
                                     DataAccountSetEditorSimple dataAccountSet, ContractAccountSetEditorSimple contractAccountSet, boolean readonly) {
		this.adminDataset = adminAccount;
		this.userAccountSet = userAccountSet;
		this.dataAccountSet = dataAccountSet;
		this.contractAccountSet = contractAccountSet;

		this.readonly = readonly;
	}

	@Override
	public LedgerAdminDataSetEditorSimple getAdminDataset() {
		return adminDataset;
	}

	@Override
	public UserAccountSetEditorSimple getUserAccountSet() {
		return userAccountSet;
	}

	@Override
	public DataAccountSetEditorSimple getDataAccountSet() {
		return dataAccountSet;
	}

	@Override
	public ContractAccountSetEditorSimple getContractAccountSet() {
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