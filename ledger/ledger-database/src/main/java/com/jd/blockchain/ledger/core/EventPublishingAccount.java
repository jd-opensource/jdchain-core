package com.jd.blockchain.ledger.core;

import java.util.Iterator;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.Event;

public class EventPublishingAccount implements EventAccount, EventPublisher {
	
	private MerkleAccount account;


	@Override
	public long publish(String eventName, BytesValue message, long sequence) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Event> getEvents(String eventName, String fromSequence, int maxCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockchainIdentity getID() {
		return account.getID();
	}

}
