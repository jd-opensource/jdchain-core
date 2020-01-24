package com.jd.blockchain.ledger.proof;

/**
 * 默克尔叶子节点的信息；
 * 
 * @author huanghaiquan
 *
 */
public interface MerkleLeaf extends MerkleElement{

	long getKeyHash();

	MerkleKey[] getKeys();

}
