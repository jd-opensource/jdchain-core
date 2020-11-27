package com.jd.blockchain.crypto.service.pki;

import org.bouncycastle.crypto.digests.SM3Digest;

import com.jd.blockchain.crypto.base.HashBaseSecureRandom;

/**
 * 采用基于 SHA1 的“哈希法”生成伪随机数；
 * 
 * @author huanghaiquan
 *
 */
class SM3SecureRandom extends HashBaseSecureRandom {

	private static final long serialVersionUID = 5750528439654395936L;
	
	public SM3SecureRandom(byte[] seed) {
		super(seed);
	}

	@Override
	protected byte[] hash(byte[] bytes) {
		SM3Digest sm3Digest = new SM3Digest();
		byte[] result = new byte[sm3Digest.getDigestSize()];
		sm3Digest.update(bytes, 0, bytes.length);
		sm3Digest.doFinal(result, 0);
		return result;
	}
}