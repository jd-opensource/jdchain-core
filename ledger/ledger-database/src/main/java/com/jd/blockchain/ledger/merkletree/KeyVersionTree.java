package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;

/**
 * 键的版本树；<br>
 * 
 * 以版本号作为编码，以值作为数据；
 * 
 * @author huanghaiquan
 *
 */
public class KeyVersionTree implements KeyIndex {

	private byte[] key;

	private MerkleSortTree<byte[]> tree;

	/**
	 * 创建新的键值索引；
	 * 
	 * @param key
	 * @param value
	 * @param bucket
	 */
	public KeyVersionTree(byte[] key, byte[] value, TreeDegree treeDegree, TreeOptions options, Bytes prefix,
			ExPolicyKVStorage kvStorage) {
		this.key = key;
		this.tree = MerkleSortTree.createBytesTree(treeDegree, options, prefix, kvStorage);
		this.tree.set(0, value);
	}

	public KeyVersionTree(byte[] key, HashDigest rootHash, TreeOptions options, Bytes prefix,
			ExPolicyKVStorage kvStorage) {
		this.key = key;
		this.tree = MerkleSortTree.createBytesTree(rootHash, options, prefix, kvStorage);
	}

	@Override
	public byte[] getKey() {
		return key;
	}

	@Override
	public HashDigest getRootHash() {
		return tree.getRootHash();
	}

	/**
	 * 返回最新版本；<br>
	 * 
	 * 如果没有数据，则返回 -1；
	 * 
	 * @return
	 */
	public long getVersion() {
		Long version = tree.getMaxId();
		return version == null ? -1 : version.longValue();
	}

	public void cancel() {
		tree.cancel();
	}

	public void commit() {
		tree.commit();
	}

	/**
	 * 返回最新版本的值；
	 * 
	 * @return
	 */
	public MerkleValue<byte[]> getValue() {
		long version = getVersion();
		return getValue(version);
	}

	/**
	 * 返回指定版本的值；
	 * <p>
	 * 如果版本不存在，则返回 null；
	 * 
	 * @param version
	 * @return
	 */
	public MerkleValue<byte[]> getValue(long version) {
		byte[] value = tree.get(version);
		if (value == null) {
			return null;
		}
		return new IDValue<byte[]>(version, value);
	}

	/**
	 * 以指定的版本号作为数据值的编码写入到版本树；
	 * 
	 * @param version
	 * @param value
	 */
	public void setValue(long version, byte[] value) {
		tree.set(version, value);
	}

}