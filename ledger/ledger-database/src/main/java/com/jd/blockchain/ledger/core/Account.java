package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.TypedValue;
import utils.Dataset;

public interface Account {
	
	BlockchainIdentity getID();
	
	Dataset<String, TypedValue> getDataset();
	
}
