package com.jd.blockchain.ledger.merkletree;

public interface BytesConverter<T> {
	
	byte[] toBytes(T value);
	
	T fromBytes(byte[] bytes);
	
}
