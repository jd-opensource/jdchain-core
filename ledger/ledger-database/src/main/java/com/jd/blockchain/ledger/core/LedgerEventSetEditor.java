package com.jd.blockchain.ledger.core;

import com.jd.blockchain.utils.Transactional;

public class LedgerEventSetEditor implements LedgerEventSet, Transactional {

	private MerkleEventSet systemEventSet;
	private EventAccountSetEditor userEventSet;
	private boolean readonly;


	public LedgerEventSetEditor(MerkleEventSet systemEventSet, EventAccountSetEditor userEventSet, boolean readonly) {
		this.systemEventSet = systemEventSet;
		this.userEventSet = userEventSet;
		this.readonly = readonly;
	}

	@Override
	public MerkleEventSet getSystemEventGroup() {
		return systemEventSet;
	}

	@Override
	public EventAccountSetEditor getEventAccountSet() {
		return userEventSet;
	}


	@Override
	public boolean isUpdated() {
		return systemEventSet.isUpdated() || userEventSet.isUpdated();
	}

	@Override
	public void commit() {
		if (readonly) {
			throw new IllegalStateException("Readonly ledger system event set which cann't been committed!");
		}
		if (!isUpdated()) {
			return;
		}

		systemEventSet.commit();
		userEventSet.commit();

	}

	@Override
	public void cancel() {
		systemEventSet.cancel();
		userEventSet.cancel();
	}

	public boolean isReadonly() {
		return readonly;
	}

//	void setReadonly() {
//		this.readonly = true;
//		this.systemEventSet.setReadonly();
//		this.userEventSet.setReadonly();
//	}
}
