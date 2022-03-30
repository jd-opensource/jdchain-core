package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.SkippingIterator;
import utils.StringUtils;
import utils.Transactional;

/**
 * @author huanghaiquan
 *
 */
public class UserAccountSetEditor implements Transactional, UserAccountSet {

	private BaseAccountSetEditor accountSet;

	public UserAccountSetEditor(CryptoSetting cryptoSetting, String keyPrefix, ExPolicyKVStorage simpleStorage,
								VersioningKVStorage versioningStorage, AccountAccessPolicy accessPolicy, LedgerDataStructure dataStructure) {
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			accountSet = new MerkleAccountSetEditor(cryptoSetting, Bytes.fromString(keyPrefix), simpleStorage, versioningStorage,
				accessPolicy);
		} else {
			accountSet = new SimpleAccountSetEditor(cryptoSetting, Bytes.fromString(keyPrefix), simpleStorage, versioningStorage,
					accessPolicy);
		}
	}

	public UserAccountSetEditor(long preBlockHeight, HashDigest dataRootHash, CryptoSetting cryptoSetting, String keyPrefix,
									  ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly, LedgerDataStructure dataStructure,
									  AccountAccessPolicy accessPolicy) {
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			accountSet = new MerkleAccountSetEditor(dataRootHash, cryptoSetting, Bytes.fromString(keyPrefix), exStorage,
					verStorage, readonly, accessPolicy);
		} else {
			accountSet = new SimpleAccountSetEditor(preBlockHeight, dataRootHash, cryptoSetting, Bytes.fromString(keyPrefix), exStorage,
				verStorage, readonly, accessPolicy);
		}
	}
	
	@Override
	public SkippingIterator<BlockchainIdentity> identityIterator() {
		return accountSet.identityIterator();
	}

	/**
	 * 返回用户总数；
	 * 
	 * @return
	 */
	@Override
	public long getTotal() {
		return accountSet.getTotal();
	}

	public boolean isReadonly() {
		return accountSet.isReadonly();
	}

//	void setReadonly() {
//		accountSet.setReadonly();
//	}

	@Override
	public HashDigest getRootHash() {
		return accountSet.getRootHash();
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return accountSet.getProof(key);
	}

	@Override
	public UserAccount getAccount(String address) {
		return getAccount(Bytes.fromBase58(address));
	}

	@Override
	public UserAccount getAccount(Bytes address) {
		CompositeAccount baseAccount = accountSet.getAccount(address);
		if(null == baseAccount) {
			return null;
		}
		return new UserAccount(baseAccount);
	}

	@Override
	public boolean contains(Bytes address) {
		return accountSet.contains(address);
	}

	@Override
	public UserAccount getAccount(Bytes address, long version) {
		CompositeAccount baseAccount = accountSet.getAccount(address, version);
		return new UserAccount(baseAccount);
	}

	/**
	 * 注册一个新用户； <br>
	 * 
	 * 如果用户已经存在，则会引发 {@link LedgerException} 异常； <br>
	 * 
	 * 如果指定的地址和公钥不匹配，则会引发 {@link LedgerException} 异常；
	 * 
	 * @param address 区块链地址；
	 * @param pubKey  公钥；
	 * @param ca  证书；
	 * @return 注册成功的用户对象；
	 */
	public UserAccount register(Bytes address, PubKey pubKey, String ca) {
		CompositeAccount baseAccount = accountSet.register(address, pubKey);
		UserAccount userAccount = new UserAccount(baseAccount);
		if(!StringUtils.isEmpty(ca)) {
			userAccount.setCertificate(ca);
		}
		return userAccount;
	}

	public UserAccount register(Bytes address, PubKey pubKey) {
		return register(address, pubKey, null);
	}

	@Override
	public boolean isUpdated() {
		return accountSet.isUpdated();
	}

	@Override
	public void commit() {
		accountSet.commit();
	}

	@Override
	public void cancel() {
		accountSet.cancel();
	}

	public void setState(Bytes address, AccountState state) {
		getAccount(address).setState(state);
	}

	public void setCertificate(Bytes address, String certificate) {
		getAccount(address).setCertificate(certificate);
	}

	// used only by kv type ledger structure, if add new account
	public boolean isAddNew() {
		return accountSet.isAddNew();
	}

	// used only by kv type ledger structure, clear accountset dataset cache index
	public void clearCachedIndex() {
		accountSet.clearCachedIndex();
	}
}