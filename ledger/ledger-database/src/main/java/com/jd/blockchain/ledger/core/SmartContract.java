package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.DataContract;

@DataContract(code=0x01)
public interface SmartContract {
	
	String getName();
	
	String getDescription();
	
}
