package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.MerkleProof;

import utils.Bytes;

public interface HashProvable {
	
	MerkleProof getProof(Bytes key);
	
}
