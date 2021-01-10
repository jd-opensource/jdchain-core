package com.jd.blockchain.ledger.proof;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.NumberEncoding;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;

/**
 * 默克尔树的路径节点的信息；
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.MERKLE_TRIE_PATH)
public interface MerklePath extends MerkleTrieEntry {

	/**
	 * 键数；
	 * 
	 * @return
	 */
	@DataField(order = 1, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG, list = true)
	long[] getChildKeys();

	/**
	 * 记录数；<br>
	 * 
	 * 一条记录是键的一个版本；所以记录数({@link #getRecords()})大于等于键数({@link #getKeySet()})；
	 * 
	 * @return
	 */
	@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG, list = true)
	long[] getChildRecords();

	/**
	 * 子节点的哈希列表；
	 * 
	 * @return
	 */
	@DataField(order = 3, primitiveType = PrimitiveType.BYTES, list = true)
	HashDigest[] getChildHashs();
}
