package com.jd.blockchain.gateway.service;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.gateway.service.search.Transaction;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.UserInfo;

public interface DataSearchService {

    /**
     * 查询所有
     *
     * @param ledgerHash
     * @param keyword
     * @return
     */
    Object searchAll(HashDigest ledgerHash, String keyword);

    /**
     * 查询数据账户数
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    int searchDataAccountCount(HashDigest ledgerHash, String address);

    /**
     * 查询数据账户
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    DataAccountInfo searchDataAccount(HashDigest ledgerHash, String address);

    /**
     * 查询事件账户数
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    int searchEventAccountCount(HashDigest ledgerHash, String address);

    /**
     * 查询事件账户
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    BlockchainIdentity searchEventAccount(HashDigest ledgerHash, String address);

    /**
     * 查询合约账户数
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    int searchContractAccountCount(HashDigest ledgerHash, String address);

    /**
     * 查询合约账户
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    ContractInfo searchContractAccount(HashDigest ledgerHash, String address);

    /**
     * 查询用户数
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    int searchUserCount(HashDigest ledgerHash, String address);

    /**
     * 查询用户
     *
     * @param ledgerHash
     * @param address
     * @return
     */
    UserInfo searchUser(HashDigest ledgerHash, String address);

    /**
     * 查询区块
     *
     * @param ledgerHash
     * @param blockHash
     * @return
     */
    LedgerBlock searchBlock(HashDigest ledgerHash, String blockHash);

    /**
     * 查询交易
     *
     * @param ledgerHash
     * @param txHash
     * @return
     */
    Transaction searchTransaction(HashDigest ledgerHash, String txHash);
}
