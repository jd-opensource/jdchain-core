package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.NumberEncoding;
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
	public BytesKVHashEntry getData(byte[] key) {
		return loadData(key, -1);
	}

	@Override
	public BytesKVHashEntry getData(byte[] key, long version) {
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
	public void setData(byte[] key, long version, byte[] dataHash) {
		// TODO Auto-generated method stub

	}
	

	private BytesKVHashEntry loadData(byte[] key, long version) {
		long keyHash = KeyIndexer.hash(key);
		HashEntry entry = hashTree.get(keyHash);
		if (entry == null) {
			return null;
		}

		if (entry instanceof BytesKVHashEntry) {
			BytesKVHashEntry dataEntry = (BytesKVHashEntry) entry;
			long dataVersion = dataEntry.getVersion();

			// 只有 version 为 0 的节点才有可能直接作为主干树的叶子节点；
			assert dataVersion == 0 : "内部数据状态错误：版本非 0 的数据节点不应该是哈希树的叶子节点!";

			if (dataVersion == version || version < 0) {
				return dataEntry;
			}
			// version > dataVersion; 指定的版本不存在，直接返回 null；
			return null;
		} else {
			// 从版本树种加载指定版本；
			HashKeySubtrees keytree;
			if (entry instanceof HashKeySubtrees) {
				keytree = (HashKeySubtrees) entry;
			}else {
				keytree = new HashKeySubtrees((KeyHashBucket) entry);
			}
			
			ValueEntry value ;
			if (version < 0) {
				value = keytree.loadMaxVersion(key);
			}else {
				value = keytree.loadVersion(key, version);
			}
			if (value == null) {
				return null;
			}
			return new BytesKeyValue(key, value.getId(), value.getBytes());
		}
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

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KV_ENTRY)
	public static interface BytesKVHashEntry extends BytesKVEntry, HashEntry {

		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		@Override
		Bytes getKey();

		@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		@Override
		long getVersion();

		@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
		@Override
		Bytes getValue();
	}

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEYINDEX)
	public static interface KeyIndex {
		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		Bytes getKey();

		@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		long getVersion();

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
			super(TreeDegree.D4, options, keyPrefix, kvStorage, new HashEntryConverter());
		}

		public HashSortTree(HashDigest rootHash, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
			super(rootHash, options, keyPrefix, kvStorage, new HashEntryConverter());
		}

	}

	/**
	 * 维护同一个 key 的多版本数据的子树；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class VersionTree extends MerkleSortTree<BytesKVHashEntry> {

		public VersionTree(TreeOptions options, String keyPrefix, ExPolicyKVStorage kvStorage) {
			super(TreeDegree.D4, options, keyPrefix, kvStorage, new KeyValueEntryConverter());
		}

		public VersionTree(HashDigest rootHash, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
			super(rootHash, options, keyPrefix, kvStorage, new KeyValueEntryConverter());
		}

	}

	private static class HashEntryConverter implements BytesConverter<HashEntry> {

		@Override
		public byte[] toBytes(HashEntry value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HashEntry fromBytes(byte[] bytes) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private static class KeyValueEntryConverter implements BytesConverter<BytesKVHashEntry> {

		@Override
		public byte[] toBytes(BytesKVHashEntry value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BytesKVHashEntry fromBytes(byte[] bytes) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private static class BytesKeyValue implements BytesKVHashEntry {

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

	private static class HashKeySubtrees implements KeyHashBucket {

		private KeyIndex[] entries;

		public HashKeySubtrees(KeyIndex keyEntry) {
			this.entries = new KeyIndex[] { keyEntry };
		}

		public HashKeySubtrees(KeyHashBucket keyset) {
			this.entries = keyset.getKeys();
		}
		

		public ValueEntry loadMaxVersion(byte[] key) {
			// TODO Auto-generated method stub
			return null;
		}

		public ValueEntry loadVersion(byte[] key, long version) {
			// TODO Auto-generated method stub
			return null;
		}
		

		public void addKey(KeyIndex hashKey) {

		}

		@Override
		public KeyIndex[] getKeys() {
			return entries;
		}

	}

}
