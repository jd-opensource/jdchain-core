package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.NumberEncoding;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;

@DataContract(code = DataCodes.MERKLE_KEY)
public interface MerkleKey extends MerkleElement {

	@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
	public byte[] getKey();
	
//	@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
	@DataField(order = 2, primitiveType = PrimitiveType.INT64)
	public long getVersion();
	
	@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
	public HashDigest getDataEntryHash();

}
