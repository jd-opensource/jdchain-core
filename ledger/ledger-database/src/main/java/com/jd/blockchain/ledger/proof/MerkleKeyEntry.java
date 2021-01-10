package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;

import utils.Bytes;

public class MerkleKeyEntry implements MerkleKey {

	/**
	 * 键；
	 */
	private Bytes key;

	/**
	 * 版本；
	 */
	private long version;

	/**
	 * 前一版本的数据节点哈希；
	 */
	private HashDigest dataEntryHash;
	
	
	
	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleKeyEntry(Bytes key, long version) {
		this(key.toBytes(), version, null);
	}
	
	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleKeyEntry(byte[] key, long version) {
		this(key, version, null);
	}

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleKeyEntry(byte[] key, long version, HashDigest dataEntryHash) {
		this(new Bytes(key), version, dataEntryHash);
	}

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleKeyEntry(Bytes key, long version, HashDigest dataEntryHash) {
		this.key = key;
		this.version = version;
		this.dataEntryHash = dataEntryHash;
	}

	@Override
	public Bytes getKey() {
		return key;
	}

	@Override
	public long getVersion() {
		return version;
	}

	void setDataEntryHash(HashDigest dataEntryHash) {
		this.dataEntryHash = dataEntryHash;
	}

	@Override
	public HashDigest getDataEntryHash() {
		return dataEntryHash;
	}

}
