package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.utils.hash.MurmurHash3;

class Murmur3HashPolicy implements KeyHashPolicy {
	
	public static final Murmur3HashPolicy INSTANCE = new Murmur3HashPolicy();

	public static final int KEY_HASHING_SEED = 2081;

	// 左 4 位为 0，用于截取哈希值的左 4 位，以免超过默克尔树的编码范围；
	private static final long CODE_MASK = 0xFFFFFFFFFFFFFFFL;
	
	private Murmur3HashPolicy() {
	}

	@Override
	public long hash(byte[] key) {
		long hashCode = MurmurHash3.murmurhash3_x64_64_2(key, 0, key.length, KEY_HASHING_SEED);
		return hashCode & CODE_MASK;
	}

}