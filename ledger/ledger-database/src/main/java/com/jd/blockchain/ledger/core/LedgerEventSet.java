package com.jd.blockchain.ledger.core;

import com.jd.blockchain.utils.Transactional;

public class LedgerEventSet implements LedgerEventQuery, Transactional {

	private MerkleEventSet systemEventSet;
	private EventAccountSet eventAccountSet;
	private boolean readonly;


	public LedgerEventSet(MerkleEventSet systemEventSet, EventAccountSet eventAccountSet, boolean readonly) {
		this.systemEventSet = systemEventSet;
		this.eventAccountSet = eventAccountSet;
		this.readonly = readonly;
	}

	@Override
	public MerkleEventSet getSystemEvents() {
		return systemEventSet;
	}

	@Override
	public EventAccountSet getUserEvents() {
		return eventAccountSet;
	}


	@Override
	public boolean isUpdated() {
		return systemEventSet.isUpdated() || eventAccountSet.isUpdated();
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
		eventAccountSet.commit();

	}

	@Override
	public void cancel() {
		systemEventSet.cancel();
		eventAccountSet.cancel();
	}

	public boolean isReadonly() {
		return readonly;
	}

	void setReadonly() {
		this.readonly = true;
		this.systemEventSet.setReadonly();
		this.eventAccountSet.setReadonly();
	}
}
