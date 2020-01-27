package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.core.MerkleProofException;

/**
 * 叶子节点；
 * 
 * @author huanghaiquan
 *
 */
class LeafNode extends MerkleTreeNode implements MerkleLeaf {

	private static final MerkleData[] EMPTY_ENTRIES = {};

	private long keyHash;

	private MerkleData[] dataEntries = EMPTY_ENTRIES;

	/**
	 * 创建一个新的叶子节点；
	 * 
	 * @param keyHash
	 */
	LeafNode(long keyHash) {
		this.keyHash = keyHash;
	}

	LeafNode(HashDigest nodeHash, MerkleLeaf leaf) {
		this.nodeHash = nodeHash;
		this.keyHash = leaf.getKeyHash();
		this.dataEntries = leaf.getDataEntries();
	}

	public void addKeyNode(MerkleDataEntry dataEntry) {
		if (dataEntries.length == 0) {
			if (dataEntry.getVersion() != 0) {
				throw new MerkleProofException("The version of the new key is not zero!");
			}
			dataEntries = new MerkleData[] { dataEntry };
		} else {
			// 按升序插入新元素；
			byte[] newKey = dataEntry.getKey();
			int i = 0;
			int count = dataEntries.length;
			int c = -1;
			for (; i < count; i++) {
				c = compare(dataEntries[i].getKey(), newKey);
				if (c >= 0) {
					break;
				}
			}
			if (c == 0) {
				// 更新 key 的新版本；
//				MerkleDataEntry entry;
//				if (dataEntries[i] instanceof MerkleDataEntry) {
////					((KeyEntry) dataEntries[i]).updateData(dataEntry);
//					entry = (MerkleDataEntry) dataEntries[i];
//				} else {
////					KeyEntry keyNode = new KeyEntry(dataEntries[i]);
////					keyNode.updateData(dataEntry);
////					dataEntries[i] = keyNode;
//					entry = new MerkleDataEntry(dataEntries[i], version, valueHash)
//				}

				dataEntry.setPreviousEntry(dataEntries[i]);
				dataEntries[i] = dataEntry;

			} else {
				// 插入新的 key；
				MerkleData[] newDataEntries = new MerkleData[count + 1];
				if (i > 0) {
					System.arraycopy(dataEntries, 0, newDataEntries, 0, i);
				}
				newDataEntries[i] = dataEntry;
				if (i < count) {
					System.arraycopy(dataEntries, i, newDataEntries, i + 1, count - i);
				}
				dataEntries = newDataEntries;
			}
		}

		setModified();
	}

	/**
	 * Compare this key and specified key;
	 * 
	 * @param otherKey
	 * @return Values: -1, 0, 1. <br>
	 *         Return -1 means that the current key is less than the specified
	 *         key;<br>
	 *         Return 0 means that the current key is equal to the specified
	 *         key;<br>
	 *         Return 1 means that the current key is great than the specified key;
	 */
	public int compare(byte[] key1, byte[] key2) {
		int len = Math.min(key1.length, key2.length);
		for (int i = 0; i < len; i++) {
			if (key1[i] == key2[i]) {
				continue;
			}
			return key1[i] < key2[i] ? -1 : 1;
		}
		if (key1.length == key2.length) {
			return 0;
		}

		return key1.length < key2.length ? -1 : 1;
	}

	public static LeafNode create(HashDigest nodeHash, MerkleLeaf leaf) {
		return new LeafNode(nodeHash, leaf);
	}

	public long getKeyHash() {
		return keyHash;
	}

	@Override
	public MerkleData[] getDataEntries() {
		return dataEntries;
	}

	@Override
	public long getTotalKeys() {
		return dataEntries.length;
	}

	@Override
	public long getTotalRecords() {
		long sum = 0;
		for (MerkleData dataEntry : dataEntries) {
			sum += dataEntry.getVersion() + 1;
		}
		return sum;
	}

	@Override
	public void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (!isModified()) {
			return;
		}

		for (MerkleData data : dataEntries) {
			if (data instanceof MerkleDataEntry) {
				MerkleDataEntry entry = (MerkleDataEntry) data;
				if (entry.getPreviousEntryHash() == null && entry.getPreviousEntry() != null) {
					HashDigest entryHash = updateDataEntry(entry.getPreviousEntry(), hashFunc, updatedListener);
					entry.setPreviousEntryHash(entryHash);
				}
			}
		}

		byte[] nodeBytes = BinaryProtocol.encode(this, MerkleLeaf.class);
		HashDigest nodeHash = hashFunc.hash(nodeBytes);
		this.nodeHash = nodeHash;

		updatedListener.onUpdated(nodeHash, this, nodeBytes);

		clearModified();
	}

	private HashDigest updateDataEntry(MerkleData data, HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (data instanceof MerkleDataEntry) {
			MerkleDataEntry entry = (MerkleDataEntry) data;
			if (entry.getPreviousEntryHash() == null && entry.getPreviousEntry() != null) {
				HashDigest entryHash = updateDataEntry(entry.getPreviousEntry(), hashFunc, updatedListener);
				entry.setPreviousEntryHash(entryHash);
			}
		}

		byte[] nodeBytes = BinaryProtocol.encode(data, MerkleData.class);
		HashDigest entryHash = hashFunc.hash(nodeBytes);
		updatedListener.onUpdated(entryHash, this, nodeBytes);

		return entryHash;
	}

}