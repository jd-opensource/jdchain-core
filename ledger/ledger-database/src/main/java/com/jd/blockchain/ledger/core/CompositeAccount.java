package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.AccountSnapshot;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.utils.Dataset;

public interface CompositeAccount extends Account, AccountSnapshot, HashProvable{

	Dataset<String, TypedValue> getHeaders();
	
}
