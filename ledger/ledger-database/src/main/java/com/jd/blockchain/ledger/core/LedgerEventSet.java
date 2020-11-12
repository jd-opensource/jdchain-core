package com.jd.blockchain.ledger.core;

public interface LedgerEventSet {

	EventGroup getSystemEventGroup();

	EventAccountSet getEventAccountSet();

}
