package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.TypedValue;

public interface AccountSimple {
	
	BlockchainIdentity getID();
	
	SimpleDataset<String, TypedValue> getDataset();
	
}
