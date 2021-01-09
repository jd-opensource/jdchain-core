package com.jd.blockchain.ledger.merkletree;

import utils.Bytes;

public class BytesKeyValue implements KVEntry, HashEntry {

	private Bytes key;
	private long version;
	private Bytes value;

	public BytesKeyValue(byte[] key, long version, byte[] value) {
		this.key = new Bytes(key);
		this.version = version;
		this.value = value == null ? null : new Bytes(value);
	}

	@Override
	public Bytes getKey() {
		return key;
	}

	@Override
	public long getVersion() {
		return version;
	}

	@Override
	public Bytes getValue() {
		return value;
	}

}