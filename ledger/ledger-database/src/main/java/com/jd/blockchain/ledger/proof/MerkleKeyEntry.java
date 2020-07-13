package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.utils.Bytes;

public class MerkleKeyEntry implements MerkleKey {

	/**
	 * 键；
	 */
	private byte[] key;


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
	public MerkleKeyEntry(Bytes key, HashDigest dataEntryHash) {
		this(key.toBytes(), dataEntryHash);
	}

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleKeyEntry(byte[] key, HashDigest dataEntryHash) {
		this.key = key;
		this.dataEntryHash = dataEntryHash;
	}

	@Override
	public byte[] getKey() {
		return key;
	}


	void setDataEntryHash(HashDigest dataEntryHash) {
		this.dataEntryHash = dataEntryHash;
	}


	@Override
	public HashDigest getDataEntryHash() {
		return dataEntryHash;
	}

}
