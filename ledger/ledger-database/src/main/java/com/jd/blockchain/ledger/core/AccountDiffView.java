package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;

public interface AccountDiffView extends DiffView {

	BlockchainIdentity getAccountID();

}