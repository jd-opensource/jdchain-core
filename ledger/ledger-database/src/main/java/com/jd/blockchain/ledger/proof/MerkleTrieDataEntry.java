package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.utils.Bytes;

public class MerkleTrieDataEntry implements MerkleTrieData {

	/**
	 * 键；
	 */
	private Bytes key;

	/**
	 * 键的版本；
	 */
	private long version;

	/**
	 * 值的哈希；
	 */
	private Bytes value;

	/**
	 * 前一版本的数据节点哈希；
	 */
	private HashDigest previousEntryHash;

	/**
	 * 前一个数据节点；
	 */
	private MerkleTrieData previousEntry;

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleTrieDataEntry(byte[] key, long version, byte[] valueHash) {
		this(new Bytes(key), version, new Bytes(valueHash));
	}
	
	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleTrieDataEntry(byte[] key, long version, Bytes valueHash) {
		this(new Bytes(key), version, valueHash);
	}

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param value 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleTrieDataEntry(Bytes key, long version, Bytes value) {
		this.key = key;
		this.version = version;
		this.value = value;
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

	@Override
	public HashDigest getPreviousEntryHash() {
		return previousEntryHash;
	}

	public MerkleTrieData getPreviousEntry() {
		return previousEntry;
	}

	void setPreviousEntry(HashDigest previousEntryHash, MerkleTrieData previousData) {
		if (this.previousEntryHash != null) {
			throw new IllegalStateException("Hash of previous data entry cann't be rewrited!");
		}

		if (this.version == 0 && previousEntryHash != null) {
			throw new IllegalStateException("Cann't set a previous data entry for the data entry with version 0!");
		}
		
		if (previousData != null && this.version != previousData.getVersion() + 1) {
			throw new MerkleProofException("The current version of data entry has not increased by 1!");
		}
		
		this.previousEntryHash = previousEntryHash;
		this.previousEntry = previousData;
	}


//	HashDigest update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
//		if (previousEntryHash == null && previousEntry != null) {
//			previousEntryHash = previousEntry.update(hashFunc, updatedListener);
//		}
//		byte[] nodeBytes = BinaryProtocol.encode(this, MerkleData.class);
//		HashDigest nodeHash = hashFunc.hash(nodeBytes);
//		updatedListener.onUpdated(nodeHash, this, nodeBytes);
//		
//		return nodeHash;
//	}

}
