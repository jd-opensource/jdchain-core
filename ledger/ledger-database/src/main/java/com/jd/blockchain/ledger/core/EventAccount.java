package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;

public interface EventAccount extends EventGroup {

	BlockchainIdentity getID();

}
