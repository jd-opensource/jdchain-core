package com.jd.blockchain.ledger.proof;

import java.util.Arrays;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.MerkleSortTree.DataPolicy;
import com.jd.blockchain.ledger.proof.MerkleSortTree.ValueEntry;
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

	private final KeyHashBucketConverter HASH_BUCKET_CONVERT;

	private MerkleSortTree<HashEntry> hashTree;

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

	@Override
	public MerkleProof getProof(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleProof getProof(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<BytesKVEntry> iterator() {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public SkippingIterator<BytesKVEntry> getKeyDiffIterator(MerkleHashTrie origTree) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BytesKVEntry getData(byte[] key) {
		return loadData(key, -1);
	}

	@Override
	public BytesKVEntry getData(byte[] key, long version) {
		return loadData(key, version);
	}

	@Override
	public long setData(byte[] key, long expectedVersion, byte[] newValue) {
		if (expectedVersion < -1) {
			throw new IllegalArgumentException("Version must be greater than or equal to -1!");
		}
		long keyHash = KeyIndexer.hash(key);

		// 写入键值数据；写入成功后，将会更新 kv 的版本；
		KVWriteEntry kv = new KVWriteEntry(key, expectedVersion, newValue);
		hashTree.set(keyHash, kv);
		return kv.getNewVersion();
	}

	private BytesKVEntry loadData(byte[] key, long version) {
		long keyHash = KeyIndexer.hash(key);
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
				// 新增数据, 预期版本应该为 -1；
				if (kvw.getExpectedVersion() != -1) {
					// 预期版本不匹配，返回失败的结果，将返回的版本号设置为 -1；
					kvw.setNewVersion(-1);
					return null;
				}

				Bytes bucketKeyPrefix = bucketPrefix.concat(BytesUtils.toBytes(id));
				bucket = new KeySetHashBucket(id, kvw.getKey(), kvw.getNewValue(), TREE_DEGREE, keyTreeOption,
						bucketKeyPrefix, kvStorage);
				// 新增数据的版本为 0 ；
				kvw.setNewVersion(bucket.getLatestVersion(kvw.getKey()));

				assert 0 == kvw.getNewVersion();

				return bucket;
			} else {
				bucket = (KeySetHashBucket) origData;
				long latestVersion = bucket.getLatestVersion(kvw.getKey());
				boolean ok = false;
				if (latestVersion == kvw.getExpectedVersion()) {
					latestVersion++;
					ok = bucket.set(kvw.getKey(), latestVersion, kvw.getNewValue());
				}
				if (ok) {
					kvw.setNewVersion(latestVersion);
					return bucket;
				} else {
					// 写入失败，设置 -1 表示失败；
					kvw.setNewVersion(-1);
					return null;
				}
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
			// TODO Auto-generated method stub
			return null;
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

			long bucketId = KeyIndexer.hash(keySet[0].getKey());
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
			this.value = new Bytes(value);
		}

		public void setVersion(long v) {
			version = v;
		}

		public BytesKeyValue(Bytes key, long version, Bytes value) {
			this.key = key;
			this.version = version;
			this.value = value;
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

	private static class KeyIndexEntry implements KeyIndex {

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
		public KeyIndexEntry(byte[] key, byte[] value, KeySetHashBucket bucket) {
			this.BUCKET = bucket;
			this.key = key;
			this.tree = createNewTree();
			this.tree.set(0, value);
		}

		public KeyIndexEntry(KeyIndex keyIndex, KeySetHashBucket bucket) {
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

		public Long getLatestVersion() {
			return tree.getMaxId();
		}

		public void cancel() {
			tree.cancel();
		}

		public void commit() {
			tree.commit();
		}

		public ValueEntry<byte[]> getLatestValue() {
			// TODO Auto-generated method stub
			return null;
		}

		public ValueEntry<byte[]> getValue(long version) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private static class KVWriteEntry implements HashEntry {

		private byte[] key;

		private long expectedVersion;

		private byte[] newValue;

		private long newVersion;

		public KVWriteEntry(byte[] key, long expectedVersion, byte[] newValue) {
			this.key = key;
			this.expectedVersion = expectedVersion;
			this.newValue = newValue;
		}

		/**
		 * 设置新的版本；
		 * 
		 * @param newVersion
		 */
		public void setNewVersion(long newVersion) {
			this.newVersion = newVersion;
		}

		public byte[] getKey() {
			return key;
		}

		public long getExpectedVersion() {
			return expectedVersion;
		}

		public byte[] getNewValue() {
			return newValue;
		}

		public long getNewVersion() {
			return newVersion;
		}

	}

	private static class KeySetHashBucket implements HashBucketEntry, HashEntry {

		private final TreeDegree treeDegree;
		private final TreeOptions treeOption;
		private final Bytes bucketKeyPrefix;
		private final ExPolicyKVStorage kvStorage;

		private final long BUCKET_ID;

		private KeyIndexEntry[] entries;

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

			KeyIndexEntry keyIndex = new KeyIndexEntry(key, value, this);

			this.entries = new KeyIndexEntry[] { keyIndex };
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

			this.entries = new KeyIndexEntry[keys.length];
			for (int i = 0; i < keys.length; i++) {
				entries[i] = new KeyIndexEntry(keys[i], this);
				long keyHash = KeyIndexer.hash(entries[i].getKey());
				if (keyHash != bucketId) {
					throw new IllegalStateException(
							"There are two keys with the two different 64 bits hash values in one hash bucket!");
				}
			}
		}

		public boolean set(byte[] key, long maxVersion, byte[] value) {
			// TODO Auto-generated method stub
			return false;
		}

		private KeyIndexEntry getKeyEntry(byte[] key) {
			// 由于哈希冲突的概率极低，绝大多数的哈希桶都只有一项 key；
			// 因此，当只有一项的时候，出于优化性能而不必对 key 进行比较，可直接返回；
			if (entries.length == 1) {
				return entries[0];
			}
			for (KeyIndexEntry keyEntry : entries) {
				if (Arrays.equals(keyEntry.getKey(), key)) {
					return keyEntry;
				}
			}
			// 正常情况下不可能执行到此处；因为至少有一个 key 会匹配；
			throw new IllegalStateException("No key match in hash bucket[" + BUCKET_ID + "]!");
		}

		public long getLatestVersion(byte[] key) {
			return getKeyEntry(key).getLatestVersion();
		}

		public void cancel() {
			for (int i = 0; i < entries.length; i++) {
				entries[i].cancel();
			}
		}

		public void commit() {
			for (int i = 0; i < entries.length; i++) {
				entries[i].commit();
			}
		}

		public ValueEntry<byte[]> loadLatestValue(byte[] key) {
			KeyIndexEntry keyEntry = getKeyEntry(key);
			return keyEntry.getLatestValue();
		}

		public ValueEntry<byte[]> loadValue(byte[] key, long version) {
			KeyIndexEntry keyEntry = getKeyEntry(key);
			return keyEntry.getValue(version);
		}

		@Override
		public KeyIndex[] getKeySet() {
			return entries;
		}

	}

	/**
	 * 返回最新 KV 的
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class LatestKeyValueIterator implements SkippingIterator<BytesKVEntry> {

		private SkippingIterator<ValueEntry> valueEntriesIterator;

		private KeyHashBucketConverter hashBucketConvert;

		@Override
		public boolean hasNext() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public BytesKVEntry next() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getTotalCount() {
			return valueEntriesIterator.getTotalCount();
		}

		@Override
		public long getCursor() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long skip(long count) {
			// TODO Auto-generated method stub
			return 0;
		}

	}

}
