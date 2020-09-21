package com.jd.blockchain.ledger.merkletree;

/**
 * KVWriteEntry 用于传递要设置的键值对信息以及返回结果；
 * 
 * @author huanghaiquan
 *
 */
class KVWriteEntry implements HashEntry {

	private byte[] key;

	private long version;

	private byte[] value;

	public KVWriteEntry(byte[] key, long version, byte[] value) {
		this.key = key;
		this.version = version;
		this.value = value;
	}

	public byte[] getKey() {
		return key;
	}

	public long getVersion() {
		return version;
	}

	public byte[] getValue() {
		return value;
	}

}