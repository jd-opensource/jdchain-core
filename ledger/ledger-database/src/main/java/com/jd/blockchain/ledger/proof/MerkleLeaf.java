package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

/**
 * 默克尔叶子节点的信息；
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.MERKLE_LEAF)
public interface MerkleLeaf extends MerkleTrieEntry{

	@DataField(order = 1, primitiveType = PrimitiveType.INT64)
	long getKeyHash();

	@DataField(order = 2, refContract = true, list = true)
	MerkleKey[] getKeys();

}
