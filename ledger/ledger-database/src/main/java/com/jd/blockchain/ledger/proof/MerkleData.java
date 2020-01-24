package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;

/**
 * Merk
 * @author huanghaiquan
 *
 */
interface MerkleData extends MerkleElement {
	
	/**
	 * 键；
	 */
	byte[] getKey();

	/**
	 * 键的版本；
	 */
	long getVersion();

	/**
	 * 值的哈希；
	 */
	HashDigest getValueHash();

	/**
	 * 记录数据的逻辑时间戳；
	 */
	long getTs();

	/**
	 * 前一版本的数据节点哈希；
	 */
	HashDigest getPreviousEntryHash();

}