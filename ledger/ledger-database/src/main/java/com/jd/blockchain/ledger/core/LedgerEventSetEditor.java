package com.jd.blockchain.ledger.core;

import utils.Transactional;

public class LedgerEventSetEditor implements LedgerEventSet, Transactional {

	private EventGroupPublisher systemEventPublisher;
	private EventAccountSetEditor userEventSet;
	private boolean readonly;


	public LedgerEventSetEditor(EventGroupPublisher systemEventSet, EventAccountSetEditor userEventSet, boolean readonly) {
		this.systemEventPublisher = systemEventSet;
		this.userEventSet = userEventSet;
		this.readonly = readonly;
	}

	@Override
	public EventGroupPublisher getSystemEventGroup() {
		return systemEventPublisher;
	}

	@Override
	public EventAccountSetEditor getEventAccountSet() {
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

	public void updatePreBlockHeight(long newBlockHeight) {
		systemEventPublisher.updatePreBlockHeight(newBlockHeight);
		userEventSet.updatePreBlockHeight(newBlockHeight);
	}

//	void setReadonly() {
//		this.readonly = true;
//		this.systemEventSet.setReadonly();
//		this.userEventSet.setReadonly();
//	}
}
