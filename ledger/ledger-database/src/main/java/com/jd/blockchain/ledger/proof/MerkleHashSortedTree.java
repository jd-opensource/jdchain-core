package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.NumberEncoding;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.MerkleSortedTree.ValueEntry;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;

public class MerkleHashSortedTree implements MerkleTree {
	
	private HashSortedTree hashTree;

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
	public MerkleProof getProof(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleProof getProof(String key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		// TODO Auto-generated method stub
		return null;
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
	public MerkleDataEntry getData(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleDataEntry getData(String key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleDataEntry getData(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleDataEntry getData(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleDataEntry getData(Bytes key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleDataEntry getData(Bytes key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleDataEntry> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleDataEntry> iterator(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleDataEntry> iterator(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleDataEntry> getKeyDiffIterator(MerkleHashTrie origTree) {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public void setData(String key, long version, byte[] data) {
////		DataEntry
//	}
//
//	@Override
//	public void setData(Bytes key, long version, byte[] data) {
//		// TODO Auto-generated method stub
//
//	}
//	
//	@Override
//	public void setData(byte[] key, long version, byte[] data) {
//		// TODO Auto-generated method stub
//
//	}

	@Override
	public void setData(String key, long version, Bytes dataHash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Bytes key, long version, Bytes dataHash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(byte[] key, long version, Bytes dataHash) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void setData(String key, long version, byte[] dataHash) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setData(Bytes key, long version, byte[] dataHash) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setData(byte[] key, long version, byte[] dataHash) {
		// TODO Auto-generated method stub
		
	}

	// ------------------- inner types ---------------------

	public static interface HashEntry {

	}

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_HASH_KV)
	public static interface HashKeyValue extends MerkleDataEntry, HashEntry {

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
	
	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_HASH_KEY)
	public static interface HashKey{
		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		Bytes getKey();

		@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		long getVersion();
		
		@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
		HashDigest getRootHash();
	}
	

	@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_HASH_KSET)
	public static interface HashKeySet extends HashEntry {

		@DataField(order = 1, refContract = true, list = true)
		HashKey[] getEntries();

	}

	/**
	 * 主干树；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class HashSortedTree extends MerkleSortedTree {

		public HashSortedTree(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
			super(TreeDegree.D4, setting, keyPrefix, kvStorage);
		}

		public HashSortedTree(HashDigest rootHash, CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
			super(rootHash, setting, keyPrefix, kvStorage);
		}

		@Override
		protected ValueEntry updateData(ValueEntry origValue, ValueEntry newValue) {
			return super.updateData(origValue, newValue);
		}

	}

	/**
	 * 维护同一个 key 的多版本数据的子树；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class VersioningDataTree extends MerkleSortedTree {

		public VersioningDataTree(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
			super(TreeDegree.D4, setting, keyPrefix, kvStorage);
		}

		public VersioningDataTree(HashDigest rootHash, CryptoSetting setting, Bytes keyPrefix,
				ExPolicyKVStorage kvStorage) {
			super(rootHash, setting, keyPrefix, kvStorage);
		}

	}

	private static class KeyValueEntry implements ValueEntry {

		private MerkleDataEntry value;

		private long id;

		public KeyValueEntry(long id, Bytes key, long version, Bytes value) {
			this.id = id;
			this.value = new DataEntry(key, version, value);
		}

		public KeyValueEntry(long id, MerkleDataEntry value) {
			this.id = id;
			this.value = value;
		}

		@Override
		public long getId() {
			return id;
		}

		@Override
		public byte[] getBytes() {
			return BinaryProtocol.encode(value, MerkleDataEntry.class);
		}

	}

	private static class DataEntry implements MerkleDataEntry {

		private Bytes key;
		private long version;
		private Bytes value;

		public DataEntry(Bytes key, long version, Bytes value) {
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

	private static class KeySetEntry implements ValueEntry {

		private HashKeySet keyset;

		private long id;

		public KeySetEntry(long id, HashKeySet keyset) {
			this.id = id;
			this.keyset = keyset;
		}

		@Override
		public long getId() {
			return id;
		}

		@Override
		public byte[] getBytes() {
			return BinaryProtocol.encode(keyset, HashKeySet.class);
		}

	}

	private static class HashKeySubtrees implements HashKeySet {

		private HashKey[] entries;

		public HashKeySubtrees(HashKey keyEntry) {
			this.entries = new HashKey[] { keyEntry };
		}
		
		public HashKeySubtrees(HashKeySet keyset) {
			this.entries = keyset.getEntries();
		}
		
		public void addKey(HashKey hashKey) {
			
		}

		@Override
		public HashKey[] getEntries() {
			return entries;
		}

	}

}
