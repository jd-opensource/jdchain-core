package com.jd.blockchain.ledger.merkletree;

/**
 * 键的哈希值的计算策略；<br>
 * 
 * 把键映射为 64 位的整数；
 * 
 * @author huanghaiquan
 *
 */
interface KeyHashPolicy {

	long hash(byte[] key);

}