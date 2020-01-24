package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;

/**
 * 默克尔树的路径节点的信息；
 * 
 * @author huanghaiquan
 *
 */
public interface MerklePath extends MerkleElement {

	/**
	 * 键数；
	 * 
	 * @return
	 */
	long[] getChildKeys();

	/**
	 * 记录数；<br>
	 * 
	 * 一条记录是键的一个版本；所以记录数({@link #getRecords()})大于等于键数({@link #getKeys()})；
	 * 
	 * @return
	 */
	long[] getChildRecords();

	/**
	 * 子节点的哈希列表；
	 * 
	 * @return
	 */
	HashDigest[] getChildHashs();
}
