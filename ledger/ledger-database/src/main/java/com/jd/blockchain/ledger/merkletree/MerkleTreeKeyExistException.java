package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.ledger.core.MerkleProofException;

public class MerkleTreeKeyExistException extends MerkleProofException {

	private static final long serialVersionUID = -5693429460488421216L;

	public MerkleTreeKeyExistException(String message) {
		super(message);
	}

}
