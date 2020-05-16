package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.utils.Bytes;

public class MerkleDataEntry implements MerkleData {

	/**
	 * 键；
	 */
	private byte[] key;

	/**
	 * 键的版本；
	 */
	private long version;

	/**
	 * 值的哈希；
	 */
	private HashDigest valueHash;

	/**
	 * 前一版本的数据节点哈希；
	 */
	private HashDigest previousEntryHash;

	/**
	 * 前一个数据节点；
	 */
	private MerkleData previousEntry;
	
	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleDataEntry(Bytes key, long version, HashDigest valueHash) {
		this(key.toBytes(), version, valueHash);
	}

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleDataEntry(byte[] key, long version, HashDigest valueHash) {
		this.key = key;
		this.version = version;
		this.valueHash = valueHash;
	}

	@Override
	public byte[] getKey() {
		return key;
	}

	@Override
	public long getVersion() {
		return version;
	}

	@Override
	public HashDigest getValueHash() {
		return valueHash;
	}

	@Override
	public HashDigest getPreviousEntryHash() {
		return previousEntryHash;
	}

	public MerkleData getPreviousEntry() {
		return previousEntry;
	}

	void setPreviousEntryHash(HashDigest previousEntryHash) {
		this.previousEntryHash = previousEntryHash;
	}

	void setPreviousEntry(MerkleData previousData) {
		if (previousEntryHash != null) {
			throw new IllegalStateException("Hash of previous data entry cann't be rewrited!");
		}
		if (version != previousData.getVersion() + 1) {
			throw new MerkleProofException("The current version of data entry has not increased by 1!");
		}
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
