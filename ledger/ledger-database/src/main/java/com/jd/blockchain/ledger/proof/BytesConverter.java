package com.jd.blockchain.ledger.proof;

public interface BytesConverter<T> {
	
	byte[] toBytes(T value);
	
	T fromBytes(byte[] bytes);
	
}
