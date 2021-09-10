package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.proof.MerkleSequenceTree;

/**
 * 根据Key未在数据存储中查到对应的数据值
 *
 */
public class DataExistException extends RuntimeException {

	private static final long serialVersionUID = 3110511167046780109L;

	public DataExistException(String message) {
		super(message);
	}

	public DataExistException(String message, Throwable cause) {
		super(message, cause);
	}

}
