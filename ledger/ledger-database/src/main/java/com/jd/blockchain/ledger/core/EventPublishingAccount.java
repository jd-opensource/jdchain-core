package com.jd.blockchain.ledger.core;

import java.util.Iterator;

import com.jd.blockchain.ledger.Account;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.TypedValue;

public class EventPublishingAccount implements EventAccount, EventPublisher {
	
	private Account account;

	public EventPublishingAccount(Account account) {
		this.account = account;
	}

	@Override
	public long publish(String eventName, BytesValue message, long sequence) {
		return account.getDataset().setValue(eventName, TypedValue.fromType(message.getType(), message.getBytes().toBytes()), sequence);
	}

	@Override
	public Iterator<Event> getEvents(String eventName, long fromSequence, int maxCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockchainIdentity getID() {
		return account.getID();
	}

}
