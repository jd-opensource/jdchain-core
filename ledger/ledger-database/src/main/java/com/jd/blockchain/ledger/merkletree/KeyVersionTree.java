package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.merkletree.MerkleSortTree.ValueEntry;
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
		Bytes keyPrefix = prefix.concat(key);
		this.tree = MerkleSortTree.createBytesTree(treeDegree, options, keyPrefix, kvStorage);
		this.tree.set(0, value);
	}

	public KeyVersionTree(KeyIndex keyIndex, TreeOptions options, Bytes prefix, ExPolicyKVStorage kvStorage) {
		this.key = keyIndex.getKey();
		Bytes keyPrefix = prefix.concat(key);
		this.tree = MerkleSortTree.createBytesTree(keyIndex.getRootHash(), options, keyPrefix, kvStorage);
	}

	@Override
	public byte[] getKey() {
		return key;
	}

	@Override
	public HashDigest getRootHash() {
		return tree.getRootHash();
	}

	public long getLatestVersion() {
		// 注：上下文逻辑和构造函数确保每一个新的 KV 树都至少有一个版本的记录，所以 MAXID 不可能为 null；
		return tree.getMaxId();
	}

	public void cancel() {
		tree.cancel();
	}

	public void commit() {
		tree.commit();
	}

	public ValueEntry<byte[]> getLatestValue() {
		long version = getLatestVersion();
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
	public ValueEntry<byte[]> getValue(long version) {
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
	public void set(long version, byte[] value) {
		tree.set(version, value);
	}

}