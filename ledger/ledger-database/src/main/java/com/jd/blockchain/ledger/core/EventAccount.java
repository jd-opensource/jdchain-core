package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.EventAccountInfo;

public interface EventAccount extends EventGroup, EventAccountInfo {

	BlockchainIdentity getID();

}
