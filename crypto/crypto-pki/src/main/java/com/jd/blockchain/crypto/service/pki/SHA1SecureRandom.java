package com.jd.blockchain.crypto.service.pki;

import org.bouncycastle.crypto.digests.SHA1Digest;

import com.jd.blockchain.crypto.base.HashBaseSecureRandom;

/**
 * 采用基于 SHA1 的“哈希法”生成伪随机数；
 * 
 * @author huanghaiquan
 *
 */
public class SHA1SecureRandom extends HashBaseSecureRandom {

	private static final long serialVersionUID = 5750528439654395936L;
	
	public static final int DIGEST_LENGTH = 20;
	
	public SHA1SecureRandom(byte[] seed) {
		super(seed);
	}
	
	@Override
	protected int getHashSize() {
		return DIGEST_LENGTH;
	}
	
	@Override
	protected void hash(byte[] bytes, byte[] output, int offset) {
		SHA1Digest sha1Digest = new SHA1Digest();
		sha1Digest.update(bytes, 0, bytes.length);
		sha1Digest.doFinal(output, offset);
	}
}