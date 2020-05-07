package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.utils.Bytes;

/**
 * @Author: zhangshuang
 * @Date: 2020/5/6 6:10 PM
 * Version 1.0
 */
public interface TxMerkleProvable extends MerkleProvable {

    /**
     * 交易数据节点的默克尔证明
     *
     *
     */
    MerkleProof getTxDataProof(Bytes key);

    /**
     * 交易状态节点的默克尔证明
     *
     *
     */
    MerkleProof getTxStateProof(Bytes key);
}
