package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.DataEntry;

public interface MerkleDataProof {
	
	DataEntry<Bytes, byte[]> getData();
	
	MerkleProof getProof();
}
