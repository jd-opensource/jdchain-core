package com.jd.blockchain.ledger.merkletree;

class BytesToBytesConverter implements BytesConverter<byte[]> {
	
	public static final BytesConverter<byte[]> INSTANCE = new BytesToBytesConverter();
	
	private BytesToBytesConverter() {
	}

	@Override
	public byte[] toBytes(byte[] value) {
		return value;
	}

	@Override
	public byte[] fromBytes(byte[] bytes) {
		return bytes;
	}
}
