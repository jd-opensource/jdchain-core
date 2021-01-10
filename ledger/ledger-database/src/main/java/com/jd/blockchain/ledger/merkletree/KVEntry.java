package com.jd.blockchain.ledger.merkletree;

import utils.Bytes;
import utils.DataEntry;

public interface KVEntry extends DataEntry<Bytes, Bytes> {
	
	@Override
	Bytes getKey();
	
	@Override
	long getVersion();
	
	@Override
	Bytes getValue();
}
