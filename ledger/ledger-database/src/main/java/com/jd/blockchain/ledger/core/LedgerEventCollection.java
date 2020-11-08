package com.jd.blockchain.ledger.core;

public interface LedgerEventCollection {
	
	EventGroup getSystemEvents();

	EventAccountCollection getUserEvents();
	
}
