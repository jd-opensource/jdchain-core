package com.jd.blockchain.ledger.merkletree;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;

@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEY_INDEX)
public interface KeyIndex {

	@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
	byte[] getKey();

	@DataField(order = 2, primitiveType = PrimitiveType.BYTES)
	HashDigest getRootHash();
}