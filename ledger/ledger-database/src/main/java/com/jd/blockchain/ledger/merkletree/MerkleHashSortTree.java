package com.jd.blockchain.ledger.merkletree;

import java.util.Arrays;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.merkletree.MerkleSortTree.DataPolicy;
import com.jd.blockchain.ledger.merkletree.MerkleSortTree.ValueEntry;
import com.jd.blockchain.ledger.proof.MerkleTree;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.AbstractSkippingIterator;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.hash.MurmurHash3;
import com.jd.blockchain.utils.io.BytesUtils;

public class MerkleHashSortTree implements MerkleTree {

	public static final KeyHashPolicy MURMUR3_HASH_POLICY = new Murmur3HashPolicy();

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
		long keyHash = MURMUR3_HASH_POLICY.hash(key);

		// 写入键值数据；写入成功后，将会更新 kv 的版本；
		KVWriteEntry kv = new KVWriteEntry(key, version, newValue);
		hashTree.set(keyHash, kv);
	}

	private BytesKVEntry loadData(byte[] key, long version) {
		long keyHash = MURMUR3_HASH_POLICY.hash(key);
		HashEntry entry = hashTree.get(keyHash);
		if (entry == null) {
			return null;
		}

		// 从版本树种加载指定版本；
		KeySetHashBucket hashBucket = (KeySetHashBucket) entry;

		ValueEntry<byte[]> value;
		if (version < 0) {
			value = hashBucket.loadLatestValue(key);
		} else {
			value = hashBucket.loadValue(key, version);
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
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static interface HashEntry {
	}

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEY_INDEX)
	public static interface KeyIndex {

		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		byte[] getKey();

		@DataField(order = 2, primitiveType = PrimitiveType.BYTES)
		HashDigest getRootHash();
	}

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEY_HASH_BUCKET)
	public static interface HashBucketEntry {

		/**
		 * 键的集合；
		 * 
		 * @return
		 */
		@DataField(order = 1, refContract = true, list = true)
		KeyIndex[] getKeySet();

	}

	/**
	 * 键的哈希值的计算策略；<br>
	 * 
	 * 把键映射为 64 位的整数；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static interface KeyHashPolicy {

		long hash(byte[] key);

	}

	private static class Murmur3HashPolicy implements KeyHashPolicy {

		public static final int KEY_HASHING_SEED = 2081;

		//左 4 位为 0，用于截取哈希值的左 4 位，以免超过默克尔树的编码范围；
		private static final long CODE_MASK = 0xFFFFFFFFFFFFFFFL;

		@Override
		public long hash(byte[] key) {
			long hashCode = MurmurHash3.murmurhash3_x64_64_2(key, 0, key.length, KEY_HASHING_SEED);
			return hashCode & CODE_MASK;
		}

	}

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

			long bucketId = MURMUR3_HASH_POLICY.hash(keySet[0].getKey());
			Bytes bucketKeyPrefix = bucketPrefix.concat(BytesUtils.toBytes(bucketId));
			return new KeySetHashBucket(bucketId, keySet, treeDegree, treeOption, bucketKeyPrefix, kvStorage);
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
	 * 键的版本树；<br>
	 * 
	 * 以版本号作为编码，以值作为数据；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class KeyVersionTree implements KeyIndex {

		private final KeySetHashBucket BUCKET;

		private byte[] key;

		private MerkleSortTree<byte[]> tree;

		/**
		 * 创建新的键值索引；
		 * 
		 * @param key
		 * @param value
		 * @param bucket
		 */
		public KeyVersionTree(byte[] key, byte[] value, KeySetHashBucket bucket) {
			this.BUCKET = bucket;
			this.key = key;
			this.tree = createNewTree();
			this.tree.set(0, value);
		}

		public KeyVersionTree(KeyIndex keyIndex, KeySetHashBucket bucket) {
			this.BUCKET = bucket;
			this.key = keyIndex.getKey();
			this.tree = createTree(keyIndex.getRootHash());
		}

		private Bytes getKeyPrefix() {
			return BUCKET.bucketKeyPrefix.concat(key);
		}

		private MerkleSortTree<byte[]> createNewTree() {
			return MerkleSortTree.createBytesTree(BUCKET.treeDegree, BUCKET.treeOption, getKeyPrefix(),
					BUCKET.kvStorage);
		}

		private MerkleSortTree<byte[]> createTree(HashDigest rootHash) {
			return MerkleSortTree.createBytesTree(rootHash, BUCKET.treeOption, getKeyPrefix(), BUCKET.kvStorage);
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

	/**
	 * KVWriteEntry 用于传递要设置的键值对信息以及返回结果；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class KVWriteEntry implements HashEntry {

		private byte[] key;

		private long version;

		private byte[] value;

		public KVWriteEntry(byte[] key, long version, byte[] value) {
			this.key = key;
			this.version = version;
			this.value = value;
		}

		public byte[] getKey() {
			return key;
		}

		public long getVersion() {
			return version;
		}

		public byte[] getValue() {
			return value;
		}

	}

	private static class KeySetHashBucket implements HashBucketEntry, HashEntry {

		private final TreeDegree treeDegree;
		private final TreeOptions treeOption;
		private final Bytes bucketKeyPrefix;
		private final ExPolicyKVStorage kvStorage;

		private final long BUCKET_ID;

		private KeyVersionTree[] kvTrees;

		/**
		 * 新创建一个新的哈希桶；
		 * 
		 * @param bucketId        哈希桶的编号；即指定的 key 的64位哈希值；
		 * @param key             哈希桶的首个键；
		 * @param value           哈希桶的首个键对应的值；
		 * @param treeDegree      键的版本子树的参数：树的度；
		 * @param treeOption      键的版本子树的参数：树的配置选项；
		 * @param bucketKeyPrefix 哈希桶的所有版本子树的共同的前缀；
		 * @param kvStorage       存储服务；
		 */
		public KeySetHashBucket(long bucketId, byte[] key, byte[] value, TreeDegree treeDegree, TreeOptions treeOption,
				Bytes bucketKeyPrefix, ExPolicyKVStorage kvStorage) {
			this.BUCKET_ID = bucketId;
			this.treeDegree = treeDegree;
			this.treeOption = treeOption;
			this.bucketKeyPrefix = bucketKeyPrefix;
			this.kvStorage = kvStorage;

			KeyVersionTree kvTree = new KeyVersionTree(key, value, this);

			this.kvTrees = new KeyVersionTree[] { kvTree };
		}

		public KeySetHashBucket(long bucketId, KeyIndex[] keys, TreeDegree treeDegree, TreeOptions treeOption,
				Bytes bucketKeyPrefix, ExPolicyKVStorage kvStorage) {
			this.BUCKET_ID = bucketId;
			this.treeDegree = treeDegree;
			this.treeOption = treeOption;
			this.bucketKeyPrefix = bucketKeyPrefix;
			this.kvStorage = kvStorage;

			if (keys.length == 0) {
				throw new IllegalStateException("No key in the specified hash bucket!");
			}

			this.kvTrees = new KeyVersionTree[keys.length];
			for (int i = 0; i < keys.length; i++) {
				kvTrees[i] = new KeyVersionTree(keys[i], this);
				long keyHash = MURMUR3_HASH_POLICY.hash(kvTrees[i].getKey());
				if (keyHash != bucketId) {
					throw new IllegalStateException(
							"There are two keys with the two different 64 bits hash values in one hash bucket!");
				}
			}
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
				kvTree = new KeyVersionTree(key, value, this);
				insertNewKey(kvTree);
			} else {
				kvTree.set(version, value);
			}

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

		public SkippingIterator<ValueEntry<HashEntry>> keysIterator() {
			return new HashBucketKeysIterator(this);
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
		 * 返回最新版本；如果不存在，则返回 -1；
		 * 
		 * @param key
		 * @return
		 */
		@SuppressWarnings("unused")
		public long getLatestVersion(byte[] key) {
			KeyVersionTree kvTree = getKeyVersionTree(key);
			if (kvTree == null) {
				return -1;
			}
			return kvTree.getLatestVersion();
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

		/**
		 * 返回指定 key 的最新版本的值；
		 * <p>
		 * 
		 * 如果 key 不存在，则返回 null；
		 * 
		 * @param key
		 * @return
		 */
		public ValueEntry<byte[]> loadLatestValue(byte[] key) {
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
		public ValueEntry<byte[]> loadValue(byte[] key, long version) {
			KeyVersionTree kvTree = getKeyVersionTree(key);
			if (kvTree == null) {
				return null;
			}
			return kvTree.getValue(version);
		}

		@Override
		public KeyIndex[] getKeySet() {
			return kvTrees;
		}

//		/**
//		 * 返回指定位置的 key 的最新版本的值；
//		 * @param index
//		 * @return
//		 */
//		public ValueEntry<byte[]> getLatestValue(int index) {
//			KeyVersionTree kvTree = kvTrees[index];
//			return kvTree.getLatestValue();
//		}

		public long getKeysCount() {
			return kvTrees.length;
		}

		public KeyVersionTree getKeyTree(int index) {
			return kvTrees[index];
		}

	}

	private static class BytesKV implements BytesKVEntry, HashEntry {

		private Bytes key;

		private long version;

		private Bytes value;

		public BytesKV(byte[] key, long version, byte[] value) {
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

	private static class HashBucketKeysIterator extends AbstractSkippingIterator<ValueEntry<HashEntry>> {

		private KeySetHashBucket bucket;

		public HashBucketKeysIterator(KeySetHashBucket bucket) {
			this.bucket = bucket;
		}

		@Override
		public long getTotalCount() {
			return bucket.getKeysCount();
		}

		@Override
		protected ValueEntry<HashEntry> get(long cursor) {
			KeyVersionTree kvTree = bucket.getKeyTree((int) cursor);
			ValueEntry<byte[]> latestValue = kvTree.getLatestValue();
			BytesKV kv = new BytesKV(kvTree.getKey(), latestValue.getId(), latestValue.getValue());
			return new IDValue<HashEntry>(bucket.BUCKET_ID, kv);
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

	private static class IDValue<T> implements ValueEntry<T> {

		private long id;

		private T value;

		private IDValue(long id, T value) {
			this.id = id;
			this.value = value;
		}

		@Override
		public long getId() {
			return id;
		}

		/**
		 * 数据字节；
		 * 
		 * @return
		 */
		@Override
		public T getValue() {
			return value;
		}

	}

}
