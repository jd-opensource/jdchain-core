package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.DataEntry;

public interface KVEntry extends DataEntry<Bytes, Bytes> {
	
	@Override
	Bytes getKey();
	
	@Override
	long getVersion();
	
	@Override
	Bytes getValue();
}
