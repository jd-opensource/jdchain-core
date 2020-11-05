package com.jd.blockchain.ledger.core;

public interface LedgerEventQuery {
	
	EventGroup getSystemEvents();

	EventAccountCollection getUserEvents();
	
}
