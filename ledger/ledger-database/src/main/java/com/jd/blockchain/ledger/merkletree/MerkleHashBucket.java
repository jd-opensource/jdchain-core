package com.jd.blockchain.ledger.merkletree;

import java.util.Arrays;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.AbstractSkippingIterator;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.EmptySkippingIterator;
import com.jd.blockchain.utils.SkippingIterator;

/**
 * 默克尔哈希桶；
 * <p>
 * 维护相同哈希值的多个 key 以及每个 key 对应的默克尔树；
 * 
 * @author huanghaiquan
 *
 */
public class MerkleHashBucket implements HashBucketEntry, HashEntry {

	private final TreeDegree treeDegree;
	private final TreeOptions treeOptions;
	private final Bytes bucketPrefix;

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
	public MerkleHashBucket(long bucketId, byte[] key, byte[] value, TreeDegree treeDegree, TreeOptions treeOptions,
			Bytes bucketPrefix, ExPolicyKVStorage kvStorage) {
		this.BUCKET_ID = bucketId;
		this.treeDegree = treeDegree;
		this.treeOptions = treeOptions;
		this.bucketPrefix = bucketPrefix;
		this.kvStorage = kvStorage;

		KeyVersionTree kvTree = createKeyTree(key, value);
		this.kvTrees = new KeyVersionTree[] { kvTree };
	}

	/**
	 * 加载哈希桶；
	 * 
	 * @param bucketId     哈希桶的编码；
	 * @param keys         与哈希桶对应的“键索引”列表；
	 * @param treeDegree   树的度；
	 * @param treeOptions
	 * @param bucketPrefix
	 * @param kvStorage
	 */
	public MerkleHashBucket(long bucketId, KeyIndex[] keys, TreeDegree treeDegree, TreeOptions treeOptions,
			Bytes bucketPrefix, ExPolicyKVStorage kvStorage) {
		this.treeDegree = treeDegree;
		this.treeOptions = treeOptions;
		this.bucketPrefix = bucketPrefix;
		this.kvStorage = kvStorage;

		if (keys.length == 0) {
			throw new IllegalStateException("No key in the specified hash bucket!");
		}

		this.kvTrees = new KeyVersionTree[keys.length];

		for (int i = 0; i < keys.length; i++) {
			byte[] key = keys[i].getKey();
			kvTrees[i] = createKeyTree(key, keys[i].getRootHash());
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
	public void setValue(byte[] key, long version, byte[] value) {
		KeyVersionTree kvTree = getKeyTree(key);
		if (kvTree == null) {
			// 新增
			kvTree = createKeyTree(key, value);
			insertNewKey(kvTree);
		} else {
			kvTree.setValue(version, value);
		}

	}

	/**
	 * 返回最新版本；如果不存在，则返回 -1；
	 * 
	 * @param key
	 * @return
	 */
	public long getVersion(byte[] key) {
		KeyVersionTree kvTree = getKeyTree(key);
		if (kvTree == null) {
			return -1;
		}
		return kvTree.getVersion();
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
	public MerkleValue<byte[]> getValue(byte[] key) {
		KeyVersionTree kvTree = getKeyTree(key);
		if (kvTree == null) {
			return null;
		}
		return kvTree.getValue();
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
	public MerkleValue<byte[]> getValue(byte[] key, long version) {
		KeyVersionTree kvTree = getKeyTree(key);
		if (kvTree == null) {
			return null;
		}
		return kvTree.getValue(version);
	}

	/**
	 * 返回所有键的最新版本值的迭代器；
	 * 
	 * @return
	 */
	public SkippingIterator<MerkleValue<HashEntry>> iterator() {
		return new BucketKVEntryIterator();
	}

	/**
	 * 返回包括指定的键的所有版本的值的迭代器；
	 * 
	 * @param key
	 * @return
	 */
	public SkippingIterator<KVEntry> iterator(byte[] key) {
		KeyVersionTree kvTree = getKeyTree(key);
		if (kvTree == null) {
			return EmptySkippingIterator.instance();
		}

		SkippingIterator<MerkleValue<byte[]>> valueIterator = kvTree.iterator();
		return new KVEntryIteratorWrapper(key, valueIterator);
	}

	/**
	 * 返回包括指定的键的所有版本的值的迭代器；
	 * 
	 * @param key
	 * @return
	 */
	public SkippingIterator<KVEntry> iterator(byte[] key, long version) {
		KeyVersionTree kvTree = getKeyTree(key);
		if (kvTree == null) {
			return EmptySkippingIterator.instance();
		}

		SkippingIterator<MerkleValue<byte[]>> valueIterator = kvTree.iterator();
		return new VersionFilterIterator(key, valueIterator, version);
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

	private KeyVersionTree createKeyTree(byte[] key, byte[] value) {
		Bytes keyTreePrefix = bucketPrefix.concat(key);
		KeyVersionTree kvTree = new KeyVersionTree(key, value, treeDegree, treeOptions, keyTreePrefix, kvStorage);
		return kvTree;
	}

	private KeyVersionTree createKeyTree(byte[] key, HashDigest rootHash) {
		Bytes keyTreePrefix = bucketPrefix.concat(key);
		KeyVersionTree kvTree = new KeyVersionTree(key, rootHash, treeOptions, keyTreePrefix, kvStorage);
		return kvTree;
	}

	private KeyVersionTree getKeyTree(int index) {
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
	private KeyVersionTree getKeyTree(byte[] key) {
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

	private class BucketKVEntryIterator extends AbstractSkippingIterator<MerkleValue<HashEntry>> {

		@Override
		public long getTotalCount() {
			return getKeysCount();
		}

		@Override
		protected MerkleValue<HashEntry> get(long cursor) {
			KeyVersionTree kvTree = getKeyTree((int) cursor);
			MerkleValue<byte[]> latestValue = kvTree.getValue();
			KeyValue kv = new KeyValue(kvTree.getKey(), latestValue.getId(), latestValue.getValue());
			return new IDValue<HashEntry>(BUCKET_ID, kv);
		}

	}

	private static class KVEntryIteratorWrapper implements SkippingIterator<KVEntry> {

		private byte[] key;

		private SkippingIterator<MerkleValue<byte[]>> values;

		public KVEntryIteratorWrapper(byte[] key, SkippingIterator<MerkleValue<byte[]>> values) {
			this.key = key;
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return values.hasNext();
		}

		@Override
		public KVEntry next() {
			MerkleValue<byte[]> value = values.next();
			if (value == null) {
				return null;
			}
			return new KeyValue(key, value.getId(), value.getValue());
		}

		@Override
		public long getTotalCount() {
			return values.getTotalCount();
		}

		@Override
		public long getCursor() {
			return values.getCursor();
		}

		@Override
		public long skip(long count) {
			return values.skip(count);
		}

	}

	private static class VersionFilterIterator extends KVEntryIteratorWrapper {

		private long totalCount;

		public VersionFilterIterator(byte[] key, SkippingIterator<MerkleValue<byte[]>> values, long maxVersion) {
			super(key, values);

			this.totalCount = Math.min(super.getTotalCount(), maxVersion + 1);
		}

		@Override
		public long getTotalCount() {
			return totalCount;
		}
	}

}