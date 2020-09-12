package com.jd.blockchain.ledger.merkletree;

public interface MerkleValue<T> {

	/**
	 * 编码；
	 * @return
	 */
	long getId();

	/**
	 * 值；
	 * 
	 * @return
	 */
	T getValue();

}