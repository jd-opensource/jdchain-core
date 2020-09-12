package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.MerkleTree;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.hash.MurmurHash3;
import com.jd.blockchain.utils.io.BytesUtils;

public class MerkleHashSortTree implements MerkleTree {

	// 左 4 位为 0，用于截取哈希值的左 4 位，以免超过默克尔树的编码范围；
	private static final long HASH_CODE_MASK = 0xFFFFFFFFFFFFFFFL;

	private static final Bytes HASH_TREE = Bytes.fromString("HASH");
	private static final Bytes BUCKET_TREE = Bytes.fromString("BUCKET");

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
		Bytes hashPrefix = prefix.concat(HASH_TREE);
		Bytes bucketPrefix = prefix.concat(BUCKET_TREE);

		this.HASH_BUCKET_CONVERT = new KeyHashBucketConverter(BUCKET_TREE_DEGREE, options, bucketPrefix, kvStorage);

		DataPolicy<HashEntry> dataPolicy = new KeyHashBucketPolicy(BUCKET_TREE_DEGREE, options, bucketPrefix,
				kvStorage);

		this.hashTree = new MerkleSortTree<HashEntry>(HASH_TREE_DEGREE, options, hashPrefix, kvStorage,
				HASH_BUCKET_CONVERT, dataPolicy);
	}

	public MerkleHashSortTree(HashDigest rootHash, TreeOptions options, Bytes prefix, ExPolicyKVStorage kvStorage) {
		Bytes hashTreePrefix = prefix.concat(HASH_TREE);
		Bytes bucketTreePrefix = prefix.concat(BUCKET_TREE);

		this.HASH_BUCKET_CONVERT = new KeyHashBucketConverter(BUCKET_TREE_DEGREE, options, bucketTreePrefix, kvStorage);

		DataPolicy<HashEntry> dataPolicy = new KeyHashBucketPolicy(BUCKET_TREE_DEGREE, options, bucketTreePrefix,
				kvStorage);

		this.hashTree = new MerkleSortTree<HashEntry>(rootHash, options, hashTreePrefix, kvStorage, HASH_BUCKET_CONVERT,
				dataPolicy);
	}

	/**
	 * 计算“键”的编码；<br>
	 * 
	 * 取“键”的64位哈希值将前 4 位置为 0 作为“键”的编码，避免超出 {@link MerkleHashSortTree} 要求的 id 范围（0 ~
	 * 2的60方）；
	 * 
	 * @param key
	 * @return
	 */
	private long computeKeyID(byte[] key) {
		return hashKey(key) & HASH_CODE_MASK;
	}

	/**
	 * 计算键的 64 位哈希值；<br>
	 * 
	 * @param key
	 * @return
	 */
	protected long hashKey(byte[] key) {
		return MurmurHash3.murmurhash3_x64_64_2(key, 0, key.length, 2281);
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
	public SkippingIterator<KVEntry> iterator() {
		return new LatestKeyValueIterator(hashTree.iterator());
	}

	@Override
	public SkippingIterator<KVEntry> iterator(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<KVEntry> iterator(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	public KVEntry getData(String key) {
		return getData(BytesUtils.toBytes(key));
	}

	@Override
	public KVEntry getData(byte[] key) {
		return loadData(key, -1);
	}

	public KVEntry getData(String key, long version) {
		return getData(BytesUtils.toBytes(key), version);
	}

	@Override
	public KVEntry getData(byte[] key, long version) {
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
		long keyHash = computeKeyID(key);

		// 写入键值数据；写入成功后，将会更新 kv 的版本；
		KVWriteEntry kv = new KVWriteEntry(key, version, newValue);
		hashTree.set(keyHash, kv);
	}

	private KVEntry loadData(byte[] key, long version) {
		long keyHash = computeKeyID(key);
		HashEntry entry = hashTree.get(keyHash);
		if (entry == null) {
			return null;
		}

		// 从版本树种加载指定版本；
		MerkleHashBucket hashBucket = (MerkleHashBucket) entry;

		MerkleValue<byte[]> value;
		if (version < 0) {
			value = hashBucket.getValue(key);
		} else {
			value = hashBucket.getValue(key, version);
		}
		if (value == null) {
			return null;
		}
		return new KeyValue(key, value.getId(), value.getValue());
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

		private Bytes treePrefix;

		private ExPolicyKVStorage kvStorage;

		public KeyHashBucketPolicy(TreeDegree treeDegree, TreeOptions keyTreeOption, Bytes bucketPrefix,
				ExPolicyKVStorage kvStorage) {
			this.TREE_DEGREE = treeDegree;
			this.keyTreeOption = keyTreeOption;
			this.treePrefix = bucketPrefix;
			this.kvStorage = kvStorage;
		}

		@Override
		public HashEntry beforeCommitting(long id, HashEntry data) {
			MerkleHashBucket bucket = (MerkleHashBucket) data;
			bucket.commit();
			return bucket;
		}

		@Override
		public void afterCanceled(long id, HashEntry data) {
			MerkleHashBucket bucket = (MerkleHashBucket) data;
			bucket.cancel();
		}

		/**
		 * 新增或者插入key；
		 */
		@Override
		public HashEntry updateData(long id, HashEntry origData, HashEntry newData) {
			KVWriteEntry kvw = (KVWriteEntry) newData;
			MerkleHashBucket bucket = null;
			if (origData == null) {
				Bytes bucketPrefix = treePrefix.concat(BytesUtils.toBytes(id));
				bucket = new MerkleHashBucket(id, kvw.getKey(), kvw.getValue(), TREE_DEGREE, keyTreeOption,
						bucketPrefix, kvStorage);
				return bucket;
			} else {
				bucket = (MerkleHashBucket) origData;
				bucket.setValue(kvw.getKey(), kvw.getVersion(), kvw.getValue());
				return bucket;
			}
		}

		@Override
		public long count(long id, HashEntry data) {
			MerkleHashBucket bucket = (MerkleHashBucket) data;
			return bucket.getKeySet().length;
		}

		@Override
		public SkippingIterator<MerkleValue<HashEntry>> iterator(long id, byte[] bytesData, long count,
				BytesConverter<HashEntry> converter) {
			MerkleHashBucket bucket = (MerkleHashBucket) converter.fromBytes(bytesData);
			SkippingIterator<MerkleValue<HashEntry>> iterator = bucket.iterator();
			assert count == iterator.getTotalCount() : "Count of data with id[" + id
					+ "] is not match the count of keys in the deserialized bucket.";
			return iterator;
		}

	}

	private class KeyHashBucketConverter implements BytesConverter<HashEntry> {

		private final TreeDegree treeDegree;
		private final TreeOptions treeOption;
		private final Bytes treePrefix;
		private final ExPolicyKVStorage kvStorage;

		public KeyHashBucketConverter(TreeDegree treeDegree, TreeOptions treeOption, Bytes treePrefix,
				ExPolicyKVStorage kvStorage) {
			this.treeDegree = treeDegree;
			this.treeOption = treeOption;
			this.treePrefix = treePrefix;
			this.kvStorage = kvStorage;
		}

		@Override
		public byte[] toBytes(HashEntry value) {
			return BinaryProtocol.encode(value, HashBucketEntry.class);
		}

		@Override
		public HashEntry fromBytes(byte[] bytes) {
			HashBucketEntry bucket = BinaryProtocol.decode(bytes, HashBucketEntry.class);

			KeyIndex[] keyset = bucket.getKeySet();

			if (keyset.length == 0) {
				throw new IllegalStateException("No key in the specified hash bucket!");
			}

			long bucketId = computeKeyID(keyset[0].getKey());
			for (int i = 1; i < keyset.length; i++) {
				long keyHash = computeKeyID(keyset[i].getKey());
				if (keyHash != bucketId) {
					throw new IllegalStateException(
							"There are two keys with the two different 64 bits hash values in one hash bucket!");
				}
			}

			Bytes bucketPrefix = treePrefix.concat(BytesUtils.toBytes(bucketId));
			return new MerkleHashBucket(bucketId, keyset, treeDegree, treeOption, bucketPrefix, kvStorage);
		}
	}

	/**
	 * 返回最新 KV 的
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class LatestKeyValueIterator implements SkippingIterator<KVEntry> {

		private SkippingIterator<MerkleValue<HashEntry>> valueEntriesIterator;

		public LatestKeyValueIterator(SkippingIterator<MerkleValue<HashEntry>> valueEntriesIterator) {
			this.valueEntriesIterator = valueEntriesIterator;
		}

		@Override
		public boolean hasNext() {
			return valueEntriesIterator.hasNext();
		}

		@Override
		public KVEntry next() {
			MerkleValue<HashEntry> value = valueEntriesIterator.next();
			if (value == null) {
				return null;
			}
			return (KVEntry) value.getValue();
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
