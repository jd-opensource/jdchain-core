package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerBlock;

public interface BlockQuery {

    /**
     * 账本Hash
     *
     * @return
     */
    HashDigest getLedgerHash();

    /**
     * 最新区块；
     *
     * @return
     */
    LedgerBlock getLatestBlock();

    /**
     * 创世区块
     *
     * @return
     */
    LedgerBlock getGenesisBlock();
}
