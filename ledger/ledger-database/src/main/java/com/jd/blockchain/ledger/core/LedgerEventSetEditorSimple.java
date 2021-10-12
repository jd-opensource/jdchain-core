package com.jd.blockchain.ledger.core;

import utils.Transactional;

public class LedgerEventSetEditorSimple implements LedgerEventSet, Transactional {

	private MerkleEventGroupPublisherSimple systemEventPublisher;
	private EventAccountSetEditorSimple userEventSet;
	private boolean readonly;


	public LedgerEventSetEditorSimple(MerkleEventGroupPublisherSimple systemEventSet, EventAccountSetEditorSimple userEventSet, boolean readonly) {
		this.systemEventPublisher = systemEventSet;
		this.userEventSet = userEventSet;
		this.readonly = readonly;
	}

	@Override
	public MerkleEventGroupPublisherSimple getSystemEventGroup() {
		return systemEventPublisher;
	}

	@Override
	public EventAccountSetEditorSimple getEventAccountSet() {
		return userEventSet;
	}


	@Override
	public boolean isUpdated() {
		return systemEventPublisher.isUpdated() || userEventSet.isUpdated();
	}

	@Override
	public void commit() {
		if (readonly) {
			throw new IllegalStateException("Readonly ledger system event set which cann't been committed!");
		}
		if (!isUpdated()) {
			return;
		}

		systemEventPublisher.commit();
		userEventSet.commit();

	}

	@Override
	public void cancel() {
		systemEventPublisher.cancel();
		userEventSet.cancel();
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void clearCachedIndex() {
		systemEventPublisher.clearCachedIndex();
		userEventSet.clearCachedIndex();
	}

//	void setReadonly() {
//		this.readonly = true;
//		this.systemEventSet.setReadonly();
//		this.userEventSet.setReadonly();
//	}
}
