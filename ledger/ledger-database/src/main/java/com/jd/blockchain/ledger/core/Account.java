package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.TypedValue;

public interface Account {
	
	BlockchainIdentity getID();
	
	MerkleDataset<String, TypedValue> getDataset();
	
}
