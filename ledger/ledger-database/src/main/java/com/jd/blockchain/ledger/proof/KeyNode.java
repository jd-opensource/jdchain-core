package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.core.MerkleProofException;

/**
 * 键节点；
 * 
 * @author huanghaiquan
 *
 */
class KeyNode implements MerkleKey {

	private byte[] key;

	private long version;

	private HashDigest dataEntryHash;

	private MerkleDataEntry dataEntry;

	public KeyNode(MerkleDataEntry dataNode) {
		this.key = dataNode.getKey();
		this.version = dataNode.getVersion();
		this.dataEntry = dataNode;
	}
	
	public KeyNode(MerkleKey merkleKey) {
		this.key = merkleKey.getKey();
		this.version = merkleKey.getVersion();
		this.dataEntryHash = merkleKey.getDataEntryHash();
	}

	public KeyNode(byte[] key, long version, HashDigest dataNodeHash) {
		assert dataNodeHash != null;

		this.key = key;
		this.version = version;
		this.dataEntryHash = dataNodeHash;
	}

	@Override
	public byte getType() {
		return KEY_ENTRY;
	}

	public MerkleData getDataNode() {
		return dataEntry;
	}

	public boolean isModified() {
		return dataEntryHash == null;
	}

	public HashDigest getDataEntryHash() {
		return dataEntryHash;
	}

	void updateData(MerkleDataEntry keyDataNode) {
		if (keyDataNode.getVersion() != (version + 1)) {
			throw new MerkleProofException("The version of key node is not increased by 1!");
		}
		keyDataNode.setPreviousNode(dataEntryHash, dataEntry);

		this.key = keyDataNode.getKey();
		this.version = keyDataNode.getVersion();
		this.dataEntryHash = null;
		this.dataEntry = keyDataNode;
	}

	public long getVersion() {
		return version;
	}

	@Override
	public byte[] getKey() {
		return key;
	}

	void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (!isModified()) {
			return;
		}

		if (dataEntry != null) {
			dataEntryHash = dataEntry.update(hashFunc, updatedListener);
//			dataEntry = null;
		}
	}

}