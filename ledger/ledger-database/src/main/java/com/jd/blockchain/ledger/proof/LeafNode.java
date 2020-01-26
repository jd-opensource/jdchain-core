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

	private static final MerkleKey[] EMPTY_KEYS = {};

	private long keyHash;

	private MerkleKey[] keys = EMPTY_KEYS;

	/**
	 * 创建一个新的叶子节点；
	 * 
	 * @param keyHash
	 */
	LeafNode(long keyHash) {
		this.keyHash = keyHash;
	}

	LeafNode(MerkleLeaf leaf) {
		this.keyHash = leaf.getKeyHash();
		this.keys = leaf.getKeys();
	}

	public void addKeyNode(MerkleDataEntry keyDataNode) {
		if (keys.length == 0) {
			if (keyDataNode.getVersion() != 0) {
				throw new MerkleProofException("The version of the new key is not zero!");
			}
			keys = new KeyEntry[] { new KeyEntry(keyDataNode) };
		} else {
			// 按升序插入新元素；
			byte[] newKey = keyDataNode.getKey();
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
				if (keys[i] instanceof KeyEntry) {
					((KeyEntry) keys[i]).updateData(keyDataNode);
				} else {
					KeyEntry keyNode = new KeyEntry(keys[i]);
					keyNode.updateData(keyDataNode);
					keys[i] = keyNode;
				}
			} else {
				// 插入新的 key；
				KeyEntry[] newKeyNodes = new KeyEntry[count + 1];
				if (i > 0) {
					System.arraycopy(keys, 0, newKeyNodes, 0, i);
				}
				newKeyNodes[i] = new KeyEntry(keyDataNode);
				if (i < count) {
					System.arraycopy(keys, i, newKeyNodes, i + 1, count - i);
				}
				keys = newKeyNodes;
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

	public static LeafNode create(HashDigest childHash, MerkleLeaf entry) {
		// TODO Auto-generated method stub
		return null;
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
		for (MerkleKey keyNode : keys) {
			sum += keyNode.getVersion()+1;
		}
		return sum;
	}

	@Override
	public void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (!isModified()) {
			return;
		}

		for (MerkleKey keyNode : keys) {
			if (keyNode instanceof KeyEntry) {
				KeyEntry kn = (KeyEntry) keyNode;
				if (kn.isModified()) {
					kn.update(hashFunc, updatedListener);
				}
			}
		}

		byte[] nodeBytes = BinaryProtocol.encode(this, MerkleLeaf.class);
		HashDigest nodeHash = hashFunc.hash(nodeBytes);
		this.nodeHash = nodeHash;

		updatedListener.onUpdated(nodeHash, this, nodeBytes);

		clearModified();
	}

}