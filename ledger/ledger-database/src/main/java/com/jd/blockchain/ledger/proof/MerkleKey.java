package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.NumberEncoding;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;

/**
 * Merk
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.MERKLE_KEY)
public interface MerkleKey extends MerkleTrieEntry {

	/**
	 * 键；
	 */
	@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
	byte[] getKey();
	
	/**
	 * 键的版本；
	 */
	@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
	long getVersion();
	
	/**
	 * 数据节点哈希；
	 */
	@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
	HashDigest getDataEntryHash();

}