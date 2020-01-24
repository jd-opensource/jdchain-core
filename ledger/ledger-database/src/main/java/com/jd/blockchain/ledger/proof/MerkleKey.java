package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;

public interface MerkleKey extends MerkleElement {

	public byte[] getKey();
	
	public long getVersion();
	
	public HashDigest getDataEntryHash();

}
