package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.utils.hash.MurmurHash3;

class KeyIndexer {

		public static final int KEY_HASHING_SEED = 117;

		private static final long[] MASK = new long[16];
		
		static {
			for (int i = 0; i < MASK.length; i++) {
				MASK[i] = 0xF << (4 * i);
			}
		}

		public static long hash(byte[] key) {
			return MurmurHash3.murmurhash3_x64_64_1(key, 0, key.length, KEY_HASHING_SEED);
		}

		public static byte index(long keyHash, int level) {
			return (byte) ((keyHash & MASK[level]) >>> (level * 4));
		}

	}