package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
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
	 * 返回指定 key 的最新数据；
	 * 
	 * @param key
	 * @return
	 */
	BytesKVEntry getData(byte[] key);

	BytesKVEntry getData(byte[] key, long version);

	/**
	 * 设置键值；
	 * 
	 * @param key             键；
	 * @param expectedVersion 当前指定的键的最新版本；只有版本匹配了，才写入值，并返回更新后的版本号；每次写入版本号递增 1；<br>
	 *                        对于不存在的键，参数应为 -1;
	 * @param newValue        要写入的新版本的值；
	 * @return 新的版本号；如果写入成功，则返回值等于 version 参数 + 1；如果版本不匹配，则返回 -1；
	 */
	long setData(byte[] key, long expectedVersion, byte[] newValue);

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

	/**
	 * 返回所有键的最新版本数据；
	 */
	SkippingIterator<BytesKVEntry> iterator();

	/**
	 * 返回指定键的所有版本数据；
	 * 
	 * @param key
	 * @return
	 */
	SkippingIterator<BytesKVEntry> iterator(byte[] key);

	/**
	 * 返回指定键的指定版本之前的所有数据（含指定版本）；
	 * 
	 * @param key
	 * @param version
	 * @return
	 */
	SkippingIterator<BytesKVEntry> iterator(byte[] key, long version);

	/**
	 * 迭代器包含所有基准树与原始树之间差异的数据项
	 */
	SkippingIterator<BytesKVEntry> getKeyDiffIterator(MerkleHashTrie origTree);

}