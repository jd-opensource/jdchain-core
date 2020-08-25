package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.Transactional;

public interface MerkleTree extends Transactional {

	/**
	 * 根哈希；
	 * 
	 * @return
	 */
	HashDigest getRootHash();

	/**
	 * 键的总数；
	 * 
	 * @return
	 */
	long getTotalKeys();

	/**
	 * 返回指定 key 最新版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	MerkleProof getProof(String key);

	/**
	 * 返回指定 key 指定版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	MerkleProof getProof(String key, long version);

	/**
	 * 返回指定 key 最新版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	MerkleProof getProof(Bytes key);

	/**
	 * 返回指定 key 最新版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	MerkleProof getProof(byte[] key);

	/**
	 * 返回指定 key 指定版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	MerkleProof getProof(byte[] key, long version);

	MerkleDataEntry getData(String key);

	MerkleDataEntry getData(String key, long version);

	MerkleDataEntry getData(byte[] key);

	MerkleDataEntry getData(byte[] key, long version);

	MerkleDataEntry getData(Bytes key);

	MerkleDataEntry getData(Bytes key, long version);

	/**
	 * 返回所有键的最新版本数据；
	 */
	SkippingIterator<MerkleDataEntry> iterator();

	/**
	 * 返回指定键的所有版本数据；
	 * 
	 * @param key
	 * @return
	 */
	SkippingIterator<MerkleDataEntry> iterator(byte[] key);

	/**
	 * 返回指定键的指定版本之前的所有数据（含指定版本）；
	 * 
	 * @param key
	 * @param version
	 * @return
	 */
	SkippingIterator<MerkleDataEntry> iterator(byte[] key, long version);

	/**
	 * 迭代器包含所有基准树与原始树之间差异的数据项
	 */
	SkippingIterator<MerkleDataEntry> getKeyDiffIterator(MerkleHashTrie origTree);

	void setData(String key, long version, byte[] data);

	void setData(Bytes key, long version, byte[] data);

	void setData(String key, long version, HashDigest dataHash);

	void setData(Bytes key, long version, HashDigest dataHash);

	void setData(byte[] key, long version, byte[] data);

	void setData(byte[] key, long version, HashDigest dataHash);

}