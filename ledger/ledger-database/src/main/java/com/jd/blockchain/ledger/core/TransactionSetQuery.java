package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;

/**
 * @Author: zhangshuang
 * @Date: 2020/5/6 4:27 PM
 * Version 1.0
 */

@DataContract(code = DataCodes.TX_SET, name = "TX-SET-QUERY")
public interface TransactionSetQuery {

    /**
     * 交易内容对应的数据集根哈希；
     *
     * @return
     */
    @DataField(order = 1, primitiveType = PrimitiveType.BYTES)
    HashDigest getTxDataSetRootHash();

    /**
     * 交易状态对应的数据集根哈希；
     *
     * @return
     */
    @DataField(order = 2, primitiveType = PrimitiveType.BYTES)
    HashDigest getTxStateSetRootHash();
}
