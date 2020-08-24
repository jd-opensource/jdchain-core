package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;

public class MerkleHashSortedTree implements MerkleTree {

	@Override
	public boolean isUpdated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub

	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

	}

	@Override
	public HashDigest getRootHash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getTotalKeys() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalRecords() {
		// TODO Auto-generated method stub
		return 0;
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
	public MerkleTrieData getData(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleTrieData getData(String key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleTrieData getData(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleTrieData getData(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleTrieData getData(Bytes key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MerkleTrieData getData(Bytes key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleTrieData> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleTrieData> iterator(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleTrieData> iterator(byte[] key, long version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SkippingIterator<MerkleTrieData> getKeyDiffIterator(MerkleHashTrie origTree) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setData(String key, long version, byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Bytes key, long version, byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(String key, long version, HashDigest dataHash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Bytes key, long version, HashDigest dataHash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(byte[] key, long version, byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(byte[] key, long version, HashDigest dataHash) {
		// TODO Auto-generated method stub

	}

	/**
	 * 主干树；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class TrunkTree extends MerkleSortedTree {

		public TrunkTree(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
			super(TreeDegree.D4, setting, keyPrefix, kvStorage);
		}

		public TrunkTree(HashDigest rootHash, CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
			super(rootHash, setting, keyPrefix, kvStorage);
		}
		
		

		@Override
		protected MerkleData updateData(MerkleData origData, MerkleData newData) {
			// TODO Auto-generated method stub
			return super.updateData(origData, newData);
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
}
