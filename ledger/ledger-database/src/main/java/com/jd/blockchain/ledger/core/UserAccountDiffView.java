package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BlockchainIdentity;

public interface UserAccountDiffView extends DiffView {

	BlockchainIdentity getAccountID();

}
