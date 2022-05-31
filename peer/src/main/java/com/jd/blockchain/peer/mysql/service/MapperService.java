package com.jd.blockchain.peer.mysql.service;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.LedgerQuery;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/14 5:53 PM
 * Version 1.0
 */
public interface MapperService {

    long getBlockTotal(HashDigest ledgerHash);

    void writeAppToMysql(LedgerQuery ledgerQuery, long blockHeight);
}
