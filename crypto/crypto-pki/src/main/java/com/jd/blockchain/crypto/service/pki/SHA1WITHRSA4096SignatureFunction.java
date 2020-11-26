package com.jd.blockchain.crypto.service.pki;

import static com.jd.blockchain.crypto.CryptoKeyType.PRIVATE;
import static com.jd.blockchain.crypto.CryptoKeyType.PUBLIC;
import static com.jd.blockchain.crypto.service.pki.PKIAlgorithm.SHA1WITHRSA2048;
import static com.jd.blockchain.crypto.service.pki.PKIAlgorithm.SHA1WITHRSA4096;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoBytes;
import com.jd.blockchain.crypto.CryptoException;
import com.jd.blockchain.crypto.CryptoKeyType;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.crypto.base.DefaultCryptoEncoding;

/**
 * @author zhanglin33
 * @title: SHA1WITHRSA4096SignatureFunction
 * @description: TODO
 * @date 2019-05-15, 17:13
 */
public class SHA1WITHRSA4096SignatureFunction implements SignatureFunction {
	private static final int RAW_PUBKEY_SIZE = 515;
	private static final int RAW_PRIVKEY_SIZE = 2307;

	private static final int RAW_SIGNATUREDIGEST_SIZE = 512;

	private static final AlgorithmIdentifier RSA_ALGORITHM_IDENTIFIER = new AlgorithmIdentifier(
			PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);

	@Override
	public SignatureDigest sign(PrivKey privKey, byte[] data) {

		byte[] rawPrivKeyBytes = privKey.getRawKeyBytes();

		if (rawPrivKeyBytes.length < RAW_PRIVKEY_SIZE) {
			throw new CryptoException("This key has wrong format!");
		}

		if (privKey.getAlgorithm() != SHA1WITHRSA4096.code()) {
			throw new CryptoException("This key is not SHA1WITHRSA4096 private key!");
		}

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(rawPrivKeyBytes);

		KeyFactory keyFactory;
		RSAPrivateCrtKey rawPrivKey;
		Signature signer;
		byte[] signature;

		try {
			keyFactory = KeyFactory.getInstance("RSA");
			rawPrivKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(keySpec);
			signer = Signature.getInstance("SHA1withRSA");
			signer.initSign(rawPrivKey);
			signer.update(data);
			signature = signer.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
			throw new CryptoException(e.getMessage(), e);
		}

		return DefaultCryptoEncoding.encodeSignatureDigest(SHA1WITHRSA4096, signature);
	}

	@Override
	public boolean verify(SignatureDigest digest, PubKey pubKey, byte[] data) {

		byte[] rawPubKeyBytes = pubKey.getRawKeyBytes();
		byte[] rawDigestBytes = digest.getRawDigest();

		if (rawPubKeyBytes.length < RAW_PUBKEY_SIZE) {
			throw new CryptoException("This key has wrong format!");
		}

		if (pubKey.getAlgorithm() != SHA1WITHRSA4096.code()) {
			throw new CryptoException("This key is not SHA1WITHRSA4096 public key!");
		}

		if (digest.getAlgorithm() != SHA1WITHRSA4096.code() || rawDigestBytes.length != RAW_SIGNATUREDIGEST_SIZE) {
			throw new CryptoException("This is not SHA1WITHRSA4096 signature digest!");
		}

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(rawPubKeyBytes);

		KeyFactory keyFactory;
		RSAPublicKey rawPubKey;
		Signature verifier;
		boolean isValid;

		try {
			keyFactory = KeyFactory.getInstance("RSA");
			rawPubKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
			verifier = Signature.getInstance("SHA1withRSA");
			verifier.initVerify(rawPubKey);
			verifier.update(data);
			isValid = verifier.verify(rawDigestBytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
			throw new CryptoException(e.getMessage(), e);
		}

		return isValid;
	}

	@Override
	public boolean supportPubKey(byte[] pubKeyBytes) {
		return pubKeyBytes.length > (CryptoAlgorithm.CODE_SIZE + CryptoKeyType.TYPE_CODE_SIZE + RAW_PUBKEY_SIZE)
				&& CryptoAlgorithm.match(SHA1WITHRSA4096, pubKeyBytes)
				&& pubKeyBytes[CryptoAlgorithm.CODE_SIZE] == PUBLIC.CODE;
	}

	@Override
	public PubKey resolvePubKey(byte[] pubKeyBytes) {
		if (supportPubKey(pubKeyBytes)) {
			return DefaultCryptoEncoding.createPubKey(SHA1WITHRSA4096.code(), pubKeyBytes);
		} else {
			throw new CryptoException("pubKeyBytes are invalid!");
		}
	}

	@Override
	public boolean supportPrivKey(byte[] privKeyBytes) {
		return privKeyBytes.length > (CryptoAlgorithm.CODE_SIZE + CryptoKeyType.TYPE_CODE_SIZE + RAW_PRIVKEY_SIZE)
				&& CryptoAlgorithm.match(SHA1WITHRSA4096, privKeyBytes)
				&& privKeyBytes[CryptoAlgorithm.CODE_SIZE] == PRIVATE.CODE;
	}

	@Override
	public PrivKey resolvePrivKey(byte[] privKeyBytes) {
		if (supportPrivKey(privKeyBytes)) {
			return DefaultCryptoEncoding.createPrivKey(SHA1WITHRSA4096.code(), privKeyBytes);
		} else {
			throw new CryptoException("privKeyBytes are invalid!");
		}
	}

	@Override
	public PubKey retrievePubKey(PrivKey privKey) {

		byte[] rawPrivKeyBytes = privKey.getRawKeyBytes();

		if (rawPrivKeyBytes.length < RAW_PRIVKEY_SIZE) {
			throw new CryptoException("This key has wrong format!");
		}

		if (privKey.getAlgorithm() != SHA1WITHRSA4096.code()) {
			throw new CryptoException("This key is not SHA1WITHRSA4096 private key!");
		}

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(rawPrivKeyBytes);

		KeyFactory keyFactory;
		RSAPrivateCrtKey rawPrivKey;
		byte[] rawPubKeyBytes;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			rawPrivKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(keySpec);
			BigInteger modulus = rawPrivKey.getModulus();
			BigInteger exponent = rawPrivKey.getPublicExponent();
			rawPubKeyBytes = KeyUtil.getEncodedSubjectPublicKeyInfo(RSA_ALGORITHM_IDENTIFIER,
					new org.bouncycastle.asn1.pkcs.RSAPublicKey(modulus, exponent));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CryptoException(e.getMessage(), e);
		}

		return DefaultCryptoEncoding.encodePubKey(SHA1WITHRSA4096, rawPubKeyBytes);
	}

	@Override
	public boolean supportDigest(byte[] digestBytes) {
		return digestBytes.length == (RAW_SIGNATUREDIGEST_SIZE + CryptoAlgorithm.CODE_SIZE)
				&& CryptoAlgorithm.match(SHA1WITHRSA4096, digestBytes);
	}

	@Override
	public SignatureDigest resolveDigest(byte[] digestBytes) {
		if (supportDigest(digestBytes)) {
			return DefaultCryptoEncoding.createSignatureDigest(SHA1WITHRSA4096.code(), digestBytes);
		} else {
			throw new CryptoException("digestBytes are invalid!");
		}
	}

	@Override
	public AsymmetricKeypair generateKeypair() {

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		KeyPairGenerator generator;
		PublicKey publicKey;
		PrivateKey privateKey;
		try {
			generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
			generator.initialize(4096);
			KeyPair keyPair = generator.generateKeyPair();
			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new CryptoException(e.getMessage(), e);
		}

		byte[] pubKeyBytes = publicKey.getEncoded();
		byte[] privKeyBytes = privateKey.getEncoded();

		PubKey pubKey = DefaultCryptoEncoding.encodePubKey(SHA1WITHRSA2048, pubKeyBytes);
		PrivKey privKey = DefaultCryptoEncoding.encodePrivKey(SHA1WITHRSA2048, privKeyBytes);

		return new AsymmetricKeypair(pubKey, privKey);
	}

	@Override
	public AsymmetricKeypair generateKeypair(byte[] seed) {
		// TODO Auto-generated method stub
		throw new IllegalStateException("Not implemented!");
	}

	@Override
	public CryptoAlgorithm getAlgorithm() {
		return SHA1WITHRSA4096;
	}

	@Override
	public <T extends CryptoBytes> boolean support(Class<T> cryptoDataType, byte[] encodedCryptoBytes) {
		return (SignatureDigest.class == cryptoDataType && supportDigest(encodedCryptoBytes))
				|| (PubKey.class == cryptoDataType && supportPubKey(encodedCryptoBytes))
				|| (PrivKey.class == cryptoDataType && supportPrivKey(encodedCryptoBytes));
	}
}
