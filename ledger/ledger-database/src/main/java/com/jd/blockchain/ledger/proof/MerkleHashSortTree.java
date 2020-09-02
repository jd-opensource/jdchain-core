package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.MerkleSortTree.ValueEntry;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.hash.MurmurHash3;

public class MerkleHashSortTree implements MerkleTree {

	private static final int KEY_HASH_SEED = 220268;

	private HashSortTree hashTree;

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
	public long setData(byte[] key, long version, byte[] value) {
		if (version < -1) {
			throw new IllegalArgumentException("Version must be greater than or equal to -1!");
		}
		long keyHash = KeyIndexer.hash(key);
		long newVersion = hashTree.set(keyHash, key, version, value);
		return newVersion;
	}

	private BytesKVEntry loadData(byte[] key, long version) {
		long keyHash = KeyIndexer.hash(key);
		HashEntry entry = hashTree.get(keyHash);
		if (entry == null) {
			return null;
		}

		// 从版本树种加载指定版本；
		KeyHashBucketTree keytree;
		if (entry instanceof KeyHashBucketTree) {
			keytree = (KeyHashBucketTree) entry;
		} else {
			keytree = new KeyHashBucketTree((KeyHashBucket) entry);
		}

		ValueEntry value;
		if (version < 0) {
			value = keytree.loadMaxVersion(key);
		} else {
			value = keytree.loadVersion(key, version);
		}
		if (value == null) {
			return null;
		}
		return new BytesKeyValue(key, value.getId(), value.getBytes());

//		if (entry instanceof BytesKVHashEntry) {
//			BytesKVHashEntry dataEntry = (BytesKVHashEntry) entry;
//			long dataVersion = dataEntry.getVersion();
//
//			// 只有 version 为 0 的节点才有可能直接作为主干树的叶子节点；
//			assert dataVersion == 0 : "内部数据状态错误：版本非 0 的数据节点不应该是哈希树的叶子节点!";
//
//			if (dataVersion == version || version < 0) {
//				return dataEntry;
//			}
//			// version > dataVersion; 指定的版本不存在，直接返回 null；
//			return null;
//		} else {
//			// 从版本树种加载指定版本；
//			KeyHashBucketTree keytree;
//			if (entry instanceof KeyHashBucketTree) {
//				keytree = (KeyHashBucketTree) entry;
//			}else {
//				keytree = new KeyHashBucketTree((KeyHashBucket) entry);
//			}
//			
//			ValueEntry value ;
//			if (version < 0) {
//				value = keytree.loadMaxVersion(key);
//			}else {
//				value = keytree.loadVersion(key, version);
//			}
//			if (value == null) {
//				return null;
//			}
//			return new BytesKeyValue(key, value.getId(), value.getBytes());
//		}
	}

	public static long computeKeyID(Bytes key) {
		return MurmurHash3.murmurhash3_x64_64_1(key, 0, key.size(), KEY_HASH_SEED);
	}

	public static long computeKeyID(byte[] key) {
		return MurmurHash3.murmurhash3_x64_64_1(key, 0, key.length, KEY_HASH_SEED);
	}

	// ------------------- inner types ---------------------

	public static interface HashEntry {

	}

//	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KV_ENTRY)
//	public static interface BytesKVHashEntry extends BytesKVEntry, HashEntry {
//
//		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
//		@Override
//		Bytes getKey();
//
//		@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
//		@Override
//		long getVersion();
//
//		@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
//		@Override
//		Bytes getValue();
//	}

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEY_INDEX)
	public static interface KeyIndex {

		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		byte[] getKey();

//		@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
//		long getVersion();

		@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
		HashDigest getRootHash();
	}

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEY_HASH_BUCKET)
	public static interface KeyHashBucket extends HashEntry {

		@DataField(order = 1, refContract = true, list = true)
		KeyIndex[] getKeys();

	}

	/**
	 * 主干树；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class HashSortTree extends MerkleSortTree<HashEntry> {

		public HashSortTree(TreeOptions options, String keyPrefix, ExPolicyKVStorage kvStorage) {
			super(TreeDegree.D4, options, keyPrefix, kvStorage, new KeyHashBucketConverter());
		}

		public HashSortTree(HashDigest rootHash, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
			super(rootHash, options, keyPrefix, kvStorage, new KeyHashBucketConverter());
		}

		@Override
		protected HashEntry beforeCommit(long id, HashEntry data) {
			KeyHashBucketTree bucket = (KeyHashBucketTree) data;
			bucket.commit();
			return bucket;
		}

		@Override
		protected void afterCancel(long id, HashEntry data) {
			KeyHashBucketTree bucket = (KeyHashBucketTree) data;
			bucket.cancel();
		}

		/**
		 * 新增或者插入key；
		 */
		@Override
		protected HashEntry updateData(long id, HashEntry origData, HashEntry newData) {
			if (origData == null) {
				//新增；
			}
			return super.updateData(id, origData, newData);
		}

		public long set(long keyHash, byte[] key, long version, byte[] value) {
			BytesKeyValue kv = new BytesKeyValue(key, version, value);

			// 写入键值数据；写入成功后，将会更新 kv 的版本；
			set(keyHash, new BytesKeyValue(key, version, value));
			
			return kv.getVersion();
		}
	}

	private static class KeyHashBucketConverter implements BytesConverter<HashEntry> {

		@Override
		public byte[] toBytes(HashEntry value) {
			return BinaryProtocol.encode(value, KeyHashBucket.class);
		}

		@Override
		public HashEntry fromBytes(byte[] bytes) {
			KeyHashBucket bucket = BinaryProtocol.decode(bytes, KeyHashBucket.class);
			return new KeyHashBucketTree(bucket);
		}

	}

	private static class BytesKeyValue implements BytesKVEntry, HashEntry {

		private Bytes key;
		private long version;
		private Bytes value;

		public BytesKeyValue(byte[] key, long version, byte[] value) {
			this.key = new Bytes(key);
			this.version = version;
			this.value = new Bytes(value);
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

		private byte[] key;

		private HashDigest rootHash;

		public KeyIndexEntry(byte[] key, HashDigest rootHash) {
			this.key = key;
			this.rootHash = rootHash;
		}

		@Override
		public byte[] getKey() {
			return key;
		}

		@Override
		public HashDigest getRootHash() {
			return rootHash;
		}

		public void setRootHash(HashDigest rootHash) {
			this.rootHash = rootHash;
		}

	}

	private static class KeyHashBucketTree implements KeyHashBucket {

		private TreeOptions treeOption;
		private Bytes bucketPrefix;
		private ExPolicyKVStorage kvStorage;

		private KeyIndexEntry[] entries;

		private MerkleSortTree<byte[]>[] keyTrees;

		public KeyHashBucketTree(byte[] key, TreeOptions treeOption, Bytes bucketPrefix, ExPolicyKVStorage kvStorage) {
			this.treeOption = treeOption;
			this.bucketPrefix = bucketPrefix;
			this.kvStorage = kvStorage;
			this.entries = new KeyIndexEntry[] { new KeyIndexEntry(key, null) };

			this.keyTrees = new MerkleSortTree[1];
		}

		public KeyHashBucketTree(KeyHashBucket keyset) {
			// TODO:
//			this.entries = keyset.getKeys();
		}

		public void cancel() {
			for (int i = 0; i < entries.length; i++) {
				if (keyTrees[i] != null) {
					keyTrees[i].cancel();
					keyTrees[i] = null;
				}
			}
		}

		public void commit() {
			for (int i = 0; i < entries.length; i++) {
				if (keyTrees[i] != null) {
					keyTrees[i].commit();
					entries[i].setRootHash(keyTrees[i].getRootHash());
				}
			}
		}

		private MerkleSortTree<byte[]> createKeyTree(TreeOptions option, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
			return MerkleSortTree.createBytesTree(TreeDegree.D4, option, keyPrefix, kvStorage);
		}

		public ValueEntry loadMaxVersion(byte[] key) {
			// TODO Auto-generated method stub
			return null;
		}

		public ValueEntry loadVersion(byte[] key, long version) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public KeyIndex[] getKeys() {
			return entries;
		}

	}

}
