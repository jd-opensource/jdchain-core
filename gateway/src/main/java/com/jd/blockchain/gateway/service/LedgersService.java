package com.jd.blockchain.gateway.service;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.TransactionService;

import java.io.Closeable;

public interface LedgersService extends Closeable {

    /**
     * 获取账本列表
     *
     * @return
     */
    HashDigest[] getLedgerHashs();

    /**
     * 获取某个账本中区块高度最高的查询器
     *
     * @param ledgerHash
     * @return
     */
    BlockchainQueryService getQueryService(HashDigest ledgerHash);

    /**
     * 获取交易处理器
     *
     * @return
     */
    TransactionService getTransactionService(HashDigest ledgerHash);

}
