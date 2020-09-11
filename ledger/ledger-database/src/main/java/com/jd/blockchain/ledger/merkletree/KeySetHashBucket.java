package com.jd.blockchain.ledger.merkletree;

import java.util.Arrays;

import com.jd.blockchain.ledger.merkletree.MerkleSortTree.ValueEntry;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.AbstractSkippingIterator;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;

public class KeySetHashBucket implements HashBucketEntry, HashEntry {

	private final TreeDegree treeDegree;
	private final TreeOptions treeOptions;
	private final Bytes bucketKeyPrefix;

	private final ExPolicyKVStorage kvStorage;

	protected final long BUCKET_ID;

	private KeyVersionTree[] kvTrees;

	/**
	 * 创建一个新的哈希桶；
	 * 
	 * @param bucketId        哈希桶的编号；即指定的 key 的64位哈希值；
	 * @param key             哈希桶的首个键；
	 * @param value           哈希桶的首个键对应的值；
	 * @param treeDegree      键的版本子树的参数：树的度；
	 * @param treeOptions     键的版本子树的参数：树的配置选项；
	 * @param bucketKeyPrefix 哈希桶的所有版本子树的共同的前缀；
	 * @param kvStorage       存储服务；
	 */
	public KeySetHashBucket(long bucketId, byte[] key, byte[] value, TreeDegree treeDegree, TreeOptions treeOptions,
			Bytes bucketKeyPrefix, ExPolicyKVStorage kvStorage) {
		this.BUCKET_ID = bucketId;
		this.treeDegree = treeDegree;
		this.treeOptions = treeOptions;
		this.bucketKeyPrefix = bucketKeyPrefix;
		this.kvStorage = kvStorage;

		KeyVersionTree kvTree = new KeyVersionTree(key, value, treeDegree, treeOptions, bucketKeyPrefix, kvStorage);

		this.kvTrees = new KeyVersionTree[] { kvTree };
	}

	/**
	 * 加载哈希桶；
	 * 
	 * @param keys
	 * @param treeDegree
	 * @param treeOptions
	 * @param bucketKeyPrefix
	 * @param kvStorage
	 */
	public KeySetHashBucket(KeyIndex[] keys, TreeDegree treeDegree, TreeOptions treeOptions, Bytes bucketKeyPrefix,
			ExPolicyKVStorage kvStorage) {
		this.treeDegree = treeDegree;
		this.treeOptions = treeOptions;
		this.bucketKeyPrefix = bucketKeyPrefix;
		this.kvStorage = kvStorage;

		if (keys.length == 0) {
			throw new IllegalStateException("No key in the specified hash bucket!");
		}

		this.kvTrees = new KeyVersionTree[keys.length];

		long bucketId = Murmur3HashPolicy.INSTANCE.hash(keys[0].getKey());
		kvTrees[0] = new KeyVersionTree(keys[0], treeOptions, bucketKeyPrefix, kvStorage);

		for (int i = 1; i < keys.length; i++) {
			long keyHash = Murmur3HashPolicy.INSTANCE.hash(kvTrees[i].getKey());
			if (keyHash != bucketId) {
				throw new IllegalStateException(
						"There are two keys with the two different 64 bits hash values in one hash bucket!");
			}

			kvTrees[i] = new KeyVersionTree(keys[i], treeOptions, bucketKeyPrefix, kvStorage);
		}

		this.BUCKET_ID = bucketId;
	}

	/**
	 * 设置指定 key 的值；
	 * 
	 * @param key     要设置的 key；
	 * @param version 版本号，作为版本子树的数据编码； ；
	 * @param value   要设置的值；
	 */
	public void set(byte[] key, long version, byte[] value) {
		KeyVersionTree kvTree = getKeyVersionTree(key);
		if (kvTree == null) {
			// 新增
			kvTree = new KeyVersionTree(key, value, treeDegree, treeOptions, bucketKeyPrefix, kvStorage);
			insertNewKey(kvTree);
		} else {
			kvTree.set(version, value);
		}

	}

	/**
	 * 返回最新版本；如果不存在，则返回 -1；
	 * 
	 * @param key
	 * @return
	 */
	public long getLatestVersion(byte[] key) {
		KeyVersionTree kvTree = getKeyVersionTree(key);
		if (kvTree == null) {
			return -1;
		}
		return kvTree.getLatestVersion();
	}

	/**
	 * 返回指定 key 的最新版本的值；
	 * <p>
	 * 
	 * 如果 key 不存在，则返回 null；
	 * 
	 * @param key
	 * @return
	 */
	public ValueEntry<byte[]> getLatestValue(byte[] key) {
		KeyVersionTree kvTree = getKeyVersionTree(key);
		if (kvTree == null) {
			return null;
		}
		return kvTree.getLatestValue();
	}

	/**
	 * 返回指定 key 的指定版本的值；
	 * <p>
	 * 
	 * 如果 key 不存在，则返回 null；
	 * 
	 * @param key
	 * @param version
	 * @return
	 */
	public ValueEntry<byte[]> getValue(byte[] key, long version) {
		KeyVersionTree kvTree = getKeyVersionTree(key);
		if (kvTree == null) {
			return null;
		}
		return kvTree.getValue(version);
	}

	/**
	 * @return
	 */
	public SkippingIterator<ValueEntry<HashEntry>> keysIterator() {
		return new HashBucketKeysIterator();
	}

	@Override
	public KeyIndex[] getKeySet() {
		return kvTrees;
	}

	public long getKeysCount() {
		return kvTrees.length;
	}

	public void cancel() {
		for (int i = 0; i < kvTrees.length; i++) {
			kvTrees[i].cancel();
		}
	}

	public void commit() {
		for (int i = 0; i < kvTrees.length; i++) {
			kvTrees[i].commit();
		}
	}

	KeyVersionTree getKeyTree(int index) {
		return kvTrees[index];
	}

	/**
	 * 返回指定 key 的版本树；
	 * <p>
	 * 
	 * 如果 key 不存在，则返回 null；
	 * 
	 * @param key
	 * @return
	 */
	private KeyVersionTree getKeyVersionTree(byte[] key) {
		for (KeyVersionTree kvTree : kvTrees) {
			if (Arrays.equals(kvTree.getKey(), key)) {
				return kvTree;
			}
		}
		return null;
	}

	/**
	 * 按照升序插入新的键；
	 * 
	 * @param kvTree
	 */
	private void insertNewKey(KeyVersionTree kvTree) {
		int i = 0;
		for (; i < kvTrees.length; i++) {
			if (Bytes.compare(kvTrees[i].getKey(), kvTree.getKey()) > 0) {
				break;
			}
		}
		KeyVersionTree[] newTrees = new KeyVersionTree[kvTrees.length + 1];
		if ((i - 0) > 0) {
			System.arraycopy(kvTrees, 0, newTrees, 0, i);
		}
		newTrees[i] = kvTree;
		if ((kvTrees.length - i) > 0) {
			System.arraycopy(kvTrees, i, newTrees, i + 1, kvTrees.length - i);
		}
		kvTrees = newTrees;
	}
	
	
	private class HashBucketKeysIterator extends AbstractSkippingIterator<ValueEntry<HashEntry>> {

		@Override
		public long getTotalCount() {
			return getKeysCount();
		}

		@Override
		protected ValueEntry<HashEntry> get(long cursor) {
			KeyVersionTree kvTree = getKeyTree((int) cursor);
			ValueEntry<byte[]> latestValue = kvTree.getLatestValue();
			BytesKV kv = new BytesKV(kvTree.getKey(), latestValue.getId(), latestValue.getValue());
			return new IDValue<HashEntry>(BUCKET_ID, kv);
		}

	}

	private class BytesKV implements BytesKVEntry, HashEntry {

		private Bytes key;

		private long version;

		private Bytes value;

		private BytesKV(byte[] key, long version, byte[] value) {
			this.key = new Bytes(key);
			this.version = version;
			this.value = new Bytes(value);
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

	}
}