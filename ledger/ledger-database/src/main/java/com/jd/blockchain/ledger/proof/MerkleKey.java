package com.jd.blockchain.ledger.proof;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.NumberEncoding;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.utils.Bytes;

/**
 * Merk
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.MERKLE_TRIE_KEY)
public interface MerkleKey extends MerkleTrieEntry {

	/**
	 * 键；
	 */
	@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
	Bytes getKey();
	
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