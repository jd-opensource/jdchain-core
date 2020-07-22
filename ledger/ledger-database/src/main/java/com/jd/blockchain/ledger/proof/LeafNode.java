package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.utils.Bytes;

/**
 * 叶子节点；
 * 
 * @author huanghaiquan
 *
 */
class LeafNode extends MerkleTreeNode implements MerkleLeaf {

	private static final MerkleKey[] EMPTY_KEYS = {};

	private static final MerkleTrieData[] EMPTY_DATA_ENTRIES = {};

	private long keyHash;

	private volatile MerkleKey[] keys;

	// 变更前的键列表；用于回滚数据；
	private volatile MerkleKey[] previousKeys;

	// 与 key 对应的数据项；用做缓存；
	private volatile MerkleTrieData[] dataEntries;

	/**
	 * 创建一个新的叶子节点；
	 * 
	 * @param keyHash
	 */
	LeafNode(long keyHash) {
		this.keyHash = keyHash;
		this.keys = EMPTY_KEYS;
		this.dataEntries = EMPTY_DATA_ENTRIES;
		this.previousKeys = EMPTY_KEYS;
		// 新节点初始便标记为“已修改”状态；
		setModified();
	}

	LeafNode(HashDigest nodeHash, MerkleLeaf leaf) {
		this.nodeHash = nodeHash;
		this.keyHash = leaf.getKeyHash();

		this.keys = leaf.getKeys();
		this.dataEntries = (this.keys == null || this.keys.length == 0) ? EMPTY_DATA_ENTRIES
				: new MerkleTrieData[keys.length];

		this.previousKeys = (this.keys == null || this.keys.length == 0) ? EMPTY_KEYS : this.keys.clone();
	}

	protected MerkleTrieData[] getDataEntries() {
		return dataEntries;
	}

	/**
	 * 插入数据；
	 * 
	 * @param index
	 * @param dataEntry
	 */
	private void insertDataEntry(int index, MerkleDataEntry dataEntry) {
		if (index < 0 || index > keys.length) {
			throw new IndexOutOfBoundsException("index of data entry is out of bounds!");
		}
		if (dataEntry.getVersion() != 0) {
			throw new MerkleProofException("The version of the new key is not zero!");
		}
		int origLen = this.keys.length;
		int newLen = origLen + 1;

		MerkleKey[] newKeys = new MerkleKey[newLen];
		MerkleTrieData[] newDatas = new MerkleTrieData[newLen];
		if (index > 0) {
			System.arraycopy(this.keys, 0, newKeys, 0, index);
			System.arraycopy(this.dataEntries, 0, newDatas, 0, index);
		}
		newKeys[index] = new MerkleKeyEntry(dataEntry.getKey(), dataEntry.getVersion(), null);// 哈希置为 null 标记此项是待写入的数据；
		newDatas[index] = dataEntry;
		if (index < this.keys.length) {
			System.arraycopy(this.keys, index, newKeys, index + 1, origLen - index);
			System.arraycopy(this.dataEntries, index, newDatas, index + 1, origLen - index);
		}
		this.keys = newKeys;
		this.dataEntries = newDatas;
	}

	/**
	 * 更改指定位置的数据项；
	 * 
	 * @param index
	 * @param dataEntry
	 */
	private void setDataEntry(int index, MerkleDataEntry dataEntry) {
		if (index < 0 || index >= keys.length) {
			throw new IndexOutOfBoundsException("index of data entry is out of bounds!");
		}
		if (dataEntry.getVersion() != (keys[index].getVersion() + 1)) {
			throw new IllegalArgumentException("The version of the key doesn't increase by 1!");
		}
		if (dataEntry.getVersion() > 0) {
			dataEntry.setPreviousEntry(this.keys[index].getDataEntryHash(), this.dataEntries[index]);
		}
		this.keys[index] = new MerkleKeyEntry(dataEntry.getKey(), dataEntry.getVersion(), null);// 哈希置为 null
		this.dataEntries[index] = dataEntry;
	}

	public void addKeyNode(MerkleDataEntry dataEntry) {
		// 在升序序列中查找合适的插入位置；
		// 如果 key 已存在，则更新数据值；否则插入数据值到匹配的位置上，把该位置之后的数据项都后移 1 位；

		if (keys.length == 0) {
			if (dataEntry.getVersion() != 0) {
				throw new MerkleProofException("The version of the new key is not zero!");
			}
			insertDataEntry(0, dataEntry);
		} else {
			// 按升序插入新元素；
			Bytes newKey = dataEntry.getKey();

			int i = 0;
			int count = keys.length;
			int c = -1;
			for (; i < count; i++) {
				c = keys[i].getKey().compare(newKey);
				if (c >= 0) {
					break;
				}
			}

			if (c == 0) {
				// 有相同的 key 存在，更新该 key 的新版本；
				setDataEntry(i, dataEntry);
			} else {
				// 没有相同的 key 存在，插入新的 key；
				insertDataEntry(i, dataEntry);
			}
		}

		setModified();
	}

	public static LeafNode create(HashDigest nodeHash, MerkleLeaf leaf) {
		return new LeafNode(nodeHash, leaf);
	}

	public long getKeyHash() {
		return keyHash;
	}

	@Override
	public MerkleKey[] getKeys() {
		return keys;
	}

	@Override
	public long getTotalKeys() {
		return keys.length;
	}

	@Override
	public long getTotalRecords() {
		long sum = 0;
		for (MerkleKey key : keys) {
			sum += key.getVersion() + 1;
		}
		return sum;
	}

	@Override
	public void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (!isModified()) {
			return;
		}

		for (int i = 0; i < keys.length; i++) {
			if (keys[i].getDataEntryHash() == null) {
				MerkleDataEntry entry = (MerkleDataEntry) dataEntries[i];
				HashDigest entryHash = updateDataEntry(entry, hashFunc, updatedListener);
				((MerkleKeyEntry) keys[i]).setDataEntryHash(entryHash);
			}
		}

		byte[] nodeBytes = BinaryProtocol.encode(this, MerkleLeaf.class);
		HashDigest nodeHash = hashFunc.hash(nodeBytes);
		this.nodeHash = nodeHash;

		updatedListener.onUpdated(nodeHash, this, nodeBytes);

		previousKeys = keys.clone();
		clearModified();
	}

	/**
	 * 更新指定的数据节点，返回节点新的哈希；
	 * @param data 数据节点；
	 * @param hashFunc 哈希函数；
	 * @param updatedListener 更新监听器；
	 * @return
	 */
	private HashDigest updateDataEntry(MerkleTrieData data, HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		//检查指定的数据节点是否存在未更新的前版本数据节点；
		if (data instanceof MerkleDataEntry) {
			MerkleDataEntry entry = (MerkleDataEntry) data;
			MerkleTrieData previousDataEntry = entry.getPreviousEntry();
			if (entry.getPreviousEntryHash() == null && previousDataEntry != null) {
				//保存前版本数据节点；
				HashDigest entryHash = updateDataEntry(previousDataEntry, hashFunc, updatedListener);
				entry.setPreviousEntry(entryHash, previousDataEntry);
			}
		}
		//更新指定的数据节点；
		byte[] nodeBytes = BinaryProtocol.encode(data, MerkleTrieData.class);
		HashDigest entryHash = hashFunc.hash(nodeBytes);
		updatedListener.onUpdated(entryHash, this, nodeBytes);

		return entryHash;
	}

	@Override
	protected void cancel() {
		if (!isModified()) {
			return;
		}
		if (previousKeys.length == 0) {
			keys = EMPTY_KEYS;
		} else {
			keys = previousKeys.clone();
		}
		clearModified();
	}
}