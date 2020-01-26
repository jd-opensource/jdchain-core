package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;

class MerkleDataEntry implements MerkleData {

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
	 * 记录数据的逻辑时间戳；
	 */
	private long ts;

	/**
	 * 前一版本的数据节点哈希；
	 */
	private HashDigest previousEntryHash;

	/**
	 * 前一个数据节点；
	 */
	private MerkleDataEntry previousEntry;

	/**
	 * @param key       键；
	 * @param version   键的版本；
	 * @param valueHash 值的哈希；
	 * @param ts        记录数据的逻辑时间戳；
	 */
	public MerkleDataEntry(byte[] key, long version, HashDigest valueHash, long ts) {
		this.key = key;
		this.version = version;
		this.valueHash = valueHash;
		this.ts = ts;
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
	public long getTs() {
		return ts;
	}

	@Override
	public HashDigest getPreviousEntryHash() {
		return previousEntryHash;
	}

	void setPreviousNode(HashDigest dataEntryHash, MerkleDataEntry dataEntry) {
		this.previousEntryHash = dataEntryHash;
		this.previousEntry = dataEntry;
	}

	HashDigest update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (previousEntryHash == null && previousEntry != null) {
			previousEntryHash = previousEntry.update(hashFunc, updatedListener);
//			previousNode = null;
		}
		byte[] nodeBytes = BinaryProtocol.encode(this, MerkleData.class);
		HashDigest nodeHash = hashFunc.hash(nodeBytes);
		updatedListener.onUpdated(nodeHash, this, nodeBytes);
		
		return nodeHash;
	}

}
