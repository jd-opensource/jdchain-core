package com.jd.blockchain.ledger.proof;

import org.springframework.context.support.EmbeddedValueResolutionSupport;

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

	private static final MerkleKey[] EMPTY_ENTRIES = {};

	private long keyHash;

	private MerkleKey[] keys;

	//变更前的键列表；用于回滚数据；
	private MerkleKey[] previousKeys;

	//与 key 对应的数据项；用做缓存；
	private MerkleData[] datas;

	/**
	 * 创建一个新的叶子节点；
	 * 
	 * @param keyHash
	 */
	LeafNode(long keyHash) {
		this.keyHash = keyHash;
		this.keys = EMPTY_ENTRIES;
		this.previousKeys = EMPTY_ENTRIES;
		
		setModified();
	}

	LeafNode(HashDigest nodeHash, MerkleLeaf leaf) {
		this.nodeHash = nodeHash;
		this.keyHash = leaf.getKeyHash();
		this.keys = leaf.getKeys();
		this.previousKeys = (this.keys == null || this.keys.length == 0) ? EMPTY_ENTRIES : this.keys.clone();
	}
	
	private MerkleDataEntry getDataEntry(byte[] key) {
		
	}
	
	/**
	 * 插入数据；
	 * @param index
	 * @param dataEntry
	 */
	private void insertDataEntry(int index, MerkleDataEntry dataEntry) {
		if (index > keys.length) {
			throw new IndexOutOfBoundsException("index of data entry is out of bounds!");
		}
		MerkleKey[] newKeys = new MerkleKey[keys.length + 1];
		if (index > 0) {
			System.arraycopy(keys, 0, newKeys, 0, index);
		}
		newKeys[index]  = new MerkleKeyEntry(dataEntry.getKey(), null);//哈希置为 null 标记此项是待写入的数据；
		if (index < keys.length) {
			System.arraycopy(keys, index, newKeys, index+1, keys.length - index);
		}
		keys = newKeys;
		
		if (datas != null) {
			
		}
	}
	
	/**
	 * 插入数据；
	 * @param index
	 * @param dataEntry
	 */
	private void updateDataEntry(int index, MerkleDataEntry dataEntry) {
		
		
	}

	public void addKeyNode(MerkleDataEntry dataEntry) {
		//在升序序列中查找合适的插入位置；
		
		//如果 key 已存在，则更新数据值；否则插入数据值到匹配的位置上，把该位置之后的数据项都后移 1 位；
		
		if (keys.length == 0) {
			if (dataEntry.getVersion() != 0) {
				throw new MerkleProofException("The version of the new key is not zero!");
			}
			keys = new MerkleKey[] { new MerkleKeyEntry(dataEntry.getKey(), dataEntryHash) };
		} else {
			// 按升序插入新元素；
			byte[] newKey = dataEntry.getKey();
			
			int i = 0;
			int count = keys.length;
			int c = -1;
			for (; i < count; i++) {
				c = compare(keys[i].getKey(), newKey);
				if (c >= 0) {
					break;
				}
			}
			
			if (c == 0) {
				// 更新 key 的新版本；
				dataEntry.setPreviousEntry(keys[i].getDataEntryHash());
				keys[i] = dataEntry;
			} else {
				// 插入新的 key；
				MerkleData[] newDataEntries = new MerkleData[count + 1];
				if (i > 0) {
					System.arraycopy(keys, 0, newDataEntries, 0, i);
				}
				newDataEntries[i] = dataEntry;
				if (i < count) {
					System.arraycopy(keys, i, newDataEntries, i + 1, count - i);
				}
				keys = newDataEntries;
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
		for (MerkleData dataEntry : keys) {
			sum += dataEntry.getVersion() + 1;
		}
		return sum;
	}

	@Override
	public void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (!isModified()) {
			return;
		}

		for (MerkleData data : keys) {
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

		previousKeys = keys.clone();
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

	@Override
	protected void cancel() {
		if (!isModified()) {
			return;
		}
		if (previousKeys.length == 0) {
			keys = EMPTY_ENTRIES;
		} else {
			keys = previousKeys.clone();
		}
		clearModified();
	}
}