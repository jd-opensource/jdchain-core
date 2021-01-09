package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.MerkleProof;

import utils.Bytes;
import utils.DataEntry;

public interface MerkleDataProof {
	
	DataEntry<Bytes, byte[]> getData();
	
	MerkleProof getProof();
}
