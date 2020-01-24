package com.jd.blockchain.ledger.proof;

public interface MerkleElement {
	
	public static final byte PATH_NODE = 0;
	
	public static final byte LEAF_NODE = 1;
	
	public static final byte KEY_ENTRY = 2;
	
	public static final byte DATA_ENTRY = 3;
	
	
	byte getType();
	
}
