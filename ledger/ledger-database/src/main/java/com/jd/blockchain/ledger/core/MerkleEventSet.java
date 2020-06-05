package com.jd.blockchain.ledger.core;

import java.util.Iterator;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.Event;
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
	 * 发布事件；<br>
	 *
	 * @param eventName 事件名；
	 * @param message 消息内容；
	 * @param sequence 事件序号；
	 * @return
	 */

	@Override
	public long publish(String eventName, BytesValue message, long sequence) {
		Bytes key = encodeKey(eventName);
		long newSequence = events.setValue(key, message.getBytes().toBytes(), sequence);

		if (newSequence < 0) {
			throw new LedgerException("Transaction is persisted repeatly! --[" + key + "]");
		}

		return newSequence;
	}

	@Override
	public Iterator<Event> getEvents(String eventName, long fromSequence, int maxCount) {
		// TODO Auto-generated method stub
		return null;
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
