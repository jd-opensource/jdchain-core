package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.merkletree.MerkleSortTree.DataPolicy;
import com.jd.blockchain.ledger.merkletree.MerkleSortTree.ValueEntry;
import com.jd.blockchain.ledger.proof.MerkleTree;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.hash.MurmurHash3;
import com.jd.blockchain.utils.io.BytesUtils;

public class MerkleHashSortTree implements MerkleTree {

	private static final int KEY_HASH_SEED = 220268;

	private static final Bytes PATH_HASH = Bytes.fromString("HASH");
	private static final Bytes PATH_BUCKET = Bytes.fromString("BUCKET");

	private static final TreeDegree HASH_TREE_DEGREE = TreeDegree.D4;

	private static final TreeDegree BUCKET_TREE_DEGREE = TreeDegree.D4;

	private static final long MAX_VERSION = BUCKET_TREE_DEGREE.MAX_COUNT;

	private final KeyHashBucketConverter HASH_BUCKET_CONVERT;

	private MerkleSortTree<HashEntry> hashTree;

	/**
	 * 创建一个空的 MerkleHashSortTree;
	 * 
	 * @param options
	 * @param prefix
	 * @param kvStorage
	 * @param keyHashPolicy
	 */
	public MerkleHashSortTree(TreeOptions options, Bytes prefix, ExPolicyKVStorage kvStorage) {
		Bytes hashPrefix = prefix.concat(PATH_HASH);
		Bytes bucketPrefix = prefix.concat(PATH_BUCKET);

		this.HASH_BUCKET_CONVERT = new KeyHashBucketConverter(BUCKET_TREE_DEGREE, options, bucketPrefix, kvStorage);

		DataPolicy<HashEntry> dataPolicy = new KeyHashBucketPolicy(BUCKET_TREE_DEGREE, options, bucketPrefix,
				kvStorage);

		this.hashTree = new MerkleSortTree<HashEntry>(HASH_TREE_DEGREE, options, hashPrefix, kvStorage,
				HASH_BUCKET_CONVERT, dataPolicy);
	}

	public MerkleHashSortTree(HashDigest rootHash, TreeOptions options, Bytes prefix, ExPolicyKVStorage kvStorage) {
		Bytes hashPrefix = prefix.concat(PATH_HASH);
		Bytes bucketPrefix = prefix.concat(PATH_BUCKET);

		this.HASH_BUCKET_CONVERT = new KeyHashBucketConverter(BUCKET_TREE_DEGREE, options, bucketPrefix, kvStorage);

		DataPolicy<HashEntry> dataPolicy = new KeyHashBucketPolicy(BUCKET_TREE_DEGREE, options, bucketPrefix,
				kvStorage);

		this.hashTree = new MerkleSortTree<HashEntry>(rootHash, options, hashPrefix, kvStorage, HASH_BUCKET_CONVERT,
				dataPolicy);
	}

	@Override
	public boolean isUpdated() {
		return hashTree.isUpdated();
	}

	@Override
	public void commit() {
		hashTree.commit();
	}

	@Override
	public void cancel() {
		hashTree.cancel();
	}

	@Override
	public HashDigest getRootHash() {
		return hashTree.getRootHash();
	}

	@Override
	public long getTotalKeys() {
		return hashTree.getCount();
	}

	public MerkleProof getProof(String key) {
		return getProof(BytesUtils.toBytes(key));
	}

	@Override
	public MerkleProof getProof(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	public MerkleProof getProof(String key, long version) {
		return getProof(BytesUtils.toBytes(key), version);
	}

	@Override
	public MerkleProof getProof(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<BytesKVEntry> iterator() {
		return new LatestKeyValueIterator(hashTree.iterator());
	}

	@Override
	public SkippingIterator<BytesKVEntry> iterator(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<BytesKVEntry> iterator(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	public BytesKVEntry getData(String key) {
		return getData(BytesUtils.toBytes(key));
	}

	@Override
	public BytesKVEntry getData(byte[] key) {
		return loadData(key, -1);
	}

	public BytesKVEntry getData(String key, long version) {
		return getData(BytesUtils.toBytes(key), version);
	}

	@Override
	public BytesKVEntry getData(byte[] key, long version) {
		return loadData(key, version);
	}

	public void setData(String key, long version, byte[] newValue) {
		setData(BytesUtils.toBytes(key), version, newValue);
	}

	@Override
	public void setData(byte[] key, long version, byte[] newValue) {
		if (version < 0) {
			throw new IllegalArgumentException("Version must be greater than or equal to zero!");
		}
		if (version >= MAX_VERSION) {
			throw new IllegalArgumentException("Version out of bound[" + MAX_VERSION + "]!");
		}
		long keyHash = Murmur3HashPolicy.INSTANCE.hash(key);

		// 写入键值数据；写入成功后，将会更新 kv 的版本；
		KVWriteEntry kv = new KVWriteEntry(key, version, newValue);
		hashTree.set(keyHash, kv);
	}

	private BytesKVEntry loadData(byte[] key, long version) {
		long keyHash = Murmur3HashPolicy.INSTANCE.hash(key);
		HashEntry entry = hashTree.get(keyHash);
		if (entry == null) {
			return null;
		}

		// 从版本树种加载指定版本；
		KeySetHashBucket hashBucket = (KeySetHashBucket) entry;

		ValueEntry<byte[]> value;
		if (version < 0) {
			value = hashBucket.getLatestValue(key);
		} else {
			value = hashBucket.getValue(key, version);
		}
		if (value == null) {
			return null;
		}
		return new BytesKeyValue(key, value.getId(), value.getValue());
	}

	public static long computeKeyID(Bytes key) {
		return MurmurHash3.murmurhash3_x64_64_1(key, 0, key.size(), KEY_HASH_SEED);
	}

	public static long computeKeyID(byte[] key) {
		return MurmurHash3.murmurhash3_x64_64_1(key, 0, key.length, KEY_HASH_SEED);
	}

	// ------------------- inner types ---------------------

	/**
	 * 哈希数据的数据策略，实现一个 key 的多版本写入；
	 * <p>
	 * 
	 * 将设置的 KVWriteEntry 转为写入到 KeySetHashBucket 的版本树；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class KeyHashBucketPolicy implements DataPolicy<HashEntry> {

		private final TreeDegree TREE_DEGREE;

		private TreeOptions keyTreeOption;

		private Bytes bucketPrefix;

		private ExPolicyKVStorage kvStorage;

		public KeyHashBucketPolicy(TreeDegree treeDegree, TreeOptions keyTreeOption, Bytes bucketPrefix,
				ExPolicyKVStorage kvStorage) {
			this.TREE_DEGREE = treeDegree;
			this.keyTreeOption = keyTreeOption;
			this.bucketPrefix = bucketPrefix;
			this.kvStorage = kvStorage;
		}

		@Override
		public HashEntry beforeCommitting(long id, HashEntry data) {
			KeySetHashBucket bucket = (KeySetHashBucket) data;
			bucket.commit();
			return bucket;
		}

		@Override
		public void afterCanceled(long id, HashEntry data) {
			KeySetHashBucket bucket = (KeySetHashBucket) data;
			bucket.cancel();
		}

		/**
		 * 新增或者插入key；
		 */
		@Override
		public HashEntry updateData(long id, HashEntry origData, HashEntry newData) {
			KVWriteEntry kvw = (KVWriteEntry) newData;
			KeySetHashBucket bucket = null;
			if (origData == null) {
				Bytes bucketKeyPrefix = bucketPrefix.concat(BytesUtils.toBytes(id));
				bucket = new KeySetHashBucket(id, kvw.getKey(), kvw.getValue(), TREE_DEGREE, keyTreeOption,
						bucketKeyPrefix, kvStorage);
				return bucket;
			} else {
				bucket = (KeySetHashBucket) origData;
				bucket.set(kvw.getKey(), kvw.getVersion(), kvw.getValue());
				return bucket;
			}
		}

		@Override
		public long count(long id, HashEntry data) {
			KeySetHashBucket bucket = (KeySetHashBucket) data;
			return bucket.getKeySet().length;
		}

		@Override
		public SkippingIterator<ValueEntry<HashEntry>> iterator(long id, byte[] bytesData, long count,
				BytesConverter<HashEntry> converter) {
			KeySetHashBucket bucket = (KeySetHashBucket) converter.fromBytes(bytesData);
			SkippingIterator<ValueEntry<HashEntry>> iterator = bucket.keysIterator();
			assert count == iterator.getTotalCount() : "Count of data with id[" + id
					+ "] is not match the count of keys in the deserialized bucket.";
			return iterator;
		}

	}

	private static class KeyHashBucketConverter implements BytesConverter<HashEntry> {

		private final TreeDegree treeDegree;
		private final TreeOptions treeOption;
		private final Bytes bucketPrefix;
		private final ExPolicyKVStorage kvStorage;

		public KeyHashBucketConverter(TreeDegree treeDegree, TreeOptions treeOption, Bytes bucketPrefix,
				ExPolicyKVStorage kvStorage) {
			this.treeDegree = treeDegree;
			this.treeOption = treeOption;
			this.bucketPrefix = bucketPrefix;
			this.kvStorage = kvStorage;
		}

		@Override
		public byte[] toBytes(HashEntry value) {
			return BinaryProtocol.encode(value, HashBucketEntry.class);
		}

		@Override
		public HashEntry fromBytes(byte[] bytes) {
			HashBucketEntry bucket = BinaryProtocol.decode(bytes, HashBucketEntry.class);

			KeyIndex[] keySet = bucket.getKeySet();
			if (keySet.length == 0) {
				throw new IllegalStateException("No key in the specified hash bucket!");
			}

			long bucketId = Murmur3HashPolicy.INSTANCE.hash(keySet[0].getKey());
			Bytes bucketKeyPrefix = bucketPrefix.concat(BytesUtils.toBytes(bucketId));
			return new KeySetHashBucket(keySet, treeDegree, treeOption, bucketKeyPrefix, kvStorage);
		}

	}

	private static class BytesKeyValue implements BytesKVEntry {

		private Bytes key;
		private long version;
		private Bytes value;

		public BytesKeyValue(byte[] key, long version, byte[] value) {
			this.key = new Bytes(key);
			this.version = version;
			this.value = value == null ? null : new Bytes(value);
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


	

	/**
	 * 返回最新 KV 的
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class LatestKeyValueIterator implements SkippingIterator<BytesKVEntry> {

		private SkippingIterator<ValueEntry<HashEntry>> valueEntriesIterator;

		public LatestKeyValueIterator(SkippingIterator<ValueEntry<HashEntry>> valueEntriesIterator) {
			this.valueEntriesIterator = valueEntriesIterator;
		}

		@Override
		public boolean hasNext() {
			return valueEntriesIterator.hasNext();
		}

		@Override
		public BytesKVEntry next() {
			ValueEntry<HashEntry> value = valueEntriesIterator.next();
			if (value == null) {
				return null;
			}
			return (BytesKVEntry) value.getValue();
		}

		@Override
		public long getTotalCount() {
			return valueEntriesIterator.getTotalCount();
		}

		@Override
		public long getCursor() {
			return valueEntriesIterator.getCursor();
		}

		@Override
		public long skip(long count) {
			return valueEntriesIterator.skip(count);
		}

	}

}
