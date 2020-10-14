package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.ledger.core.MerkleProofException;

/**
 * 默克尔键值存储冲突异常； 
 * 
 * @author huanghaiquan
 *
 */
public class MerkleKeyStorageConflictException extends MerkleProofException{
	
	private static final long serialVersionUID = 7099663821244946482L;

	public MerkleKeyStorageConflictException(String message) {
		super(message);
	}

	public MerkleKeyStorageConflictException(String message, Throwable cause) {
		super(message, cause);
	}
}
