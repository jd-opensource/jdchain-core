package com.jd.blockchain.ledger.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;

public class MerkleEventSet implements EventGroup, EventPublisher, Transactional {

	private MerkleDataSet events;

	public MerkleEventSet(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage, VersioningKVStorage verStorage) {
		events = new MerkleDataSet(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage);
	}

	public MerkleEventSet(HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
						   ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly) {
		events = new MerkleDataSet(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, readonly);
	}

	/**
	 * 发布事件
	 * @param event
	 * @return
	 */
	@Override
	public long publish(Event event) {
		Bytes key = encodeKey(event.getName());
		long newSequence = events.setValue(key, BinaryProtocol.encode(event, Event.class), event.getSequence()-1);

		if (newSequence < 0) {
			throw new LedgerException("Transaction is persisted repeatly! --[" + key + "]");
		}

		return newSequence;
	}

	@Override
	public Iterator<Event> getEvents(String eventName, long fromSequence, int maxCount) {
		List<Event> eventsArray = new ArrayList<>();
		Bytes key = encodeKey(eventName);
		long maxVersion = events.getVersion(key);
		for (int i = 0; i < maxCount && i <= maxVersion; i++) {
			byte[] bs = events.getValue(key, fromSequence + i);
			if (null == bs) {
			    break;
            }
			Event event = BinaryProtocol.decode(bs);
			eventsArray.add(new EventInfo(event));
		}
		return eventsArray.iterator();
	}

	public HashDigest getRootHash() {
		return events.getRootHash();
	}

	private Bytes encodeKey(String eventName) {
		return Bytes.fromString(eventName);
	}

	@Override
	public boolean isUpdated() {
		return events.isUpdated();
	}

	@Override
	public void commit() {
		events.commit();
	}

	@Override
	public void cancel() {
		events.cancel();
	}

	public boolean isReadonly() {
		return events.isReadonly();
	}

	void setReadonly() {
		events.setReadonly();
	}
}
