package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.cache.ContractCache;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.SkippingIterator;
import utils.Transactional;

public class ContractAccountSetEditor implements Transactional, ContractAccountSet {

	private BaseAccountSetEditor accountSet;
	private ContractCache cache;

	public ContractAccountSetEditor(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage,
			VersioningKVStorage verStorage, AccountAccessPolicy accessPolicy, LedgerDataStructure dataStructure, ContractCache cache) {
		this.cache = cache;
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			accountSet = new MerkleAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, cache, accessPolicy);
		} else {
			accountSet = new KvAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, accessPolicy, DatasetType.CONTS);
		}
	}

	public ContractAccountSetEditor(long preBlockHeight, HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
										  ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly, LedgerDataStructure dataStructure,
									ContractCache cache, AccountAccessPolicy accessPolicy) {
		this.cache = cache;
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			accountSet = new MerkleAccountSetEditor(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
					readonly, cache, accessPolicy);
		} else {
			accountSet = new KvAccountSetEditor(preBlockHeight, dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
					readonly, accessPolicy, DatasetType.CONTS);
		}
	}
	
	@Override
	public SkippingIterator<BlockchainIdentity> identityIterator() {
		return accountSet.identityIterator();
	}

	public boolean isReadonly() {
		return accountSet.isReadonly();
	}


	@Override
	public HashDigest getRootHash() {
		return accountSet.getRootHash();
	}

	/**
	 * 返回合约总数；
	 * 
	 * @return
	 */
	@Override
	public long getTotal() {
		return accountSet.getTotal();
	}

	@Override
	public MerkleProof getProof(Bytes address) {
		return accountSet.getProof(address);
	}

	@Override
	public boolean contains(Bytes address) {
		return accountSet.contains(address);
	}

	@Override
	public ContractAccount getAccount(Bytes address) {
		CompositeAccount accBase = accountSet.getAccount(address);
		if(null == accBase) {
			return null;
		}
		return new ContractAccount(accBase, cache);
	}

	@Override
	public ContractAccount getAccount(String address) {
		return getAccount(Bytes.fromBase58(address));
	}

	@Override
	public ContractAccount getAccount(Bytes address, long version) {
		CompositeAccount accBase = accountSet.getAccount(address, version);
		if(null == accBase) {
			return null;
		}
		return new ContractAccount(accBase, cache);
	}

	/**
	 * 部署一项新的合约链码；
	 * 
	 * @param address          合约账户地址；
	 * @param pubKey           合约账户公钥；
	 * @param addressSignature 地址签名；合约账户的私钥对地址的签名；
	 * @param chaincode        链码内容；
	 * @param lang   合约语言；
	 * @return 合约账户；
	 */
	public ContractAccount deploy(Bytes address, PubKey pubKey, DigitalSignature addressSignature, byte[] chaincode, ContractLang lang) {
		// TODO: 校验和记录合约地址签名；
		//is exist address?
		ContractAccount contractAcc;
		if(!accountSet.contains(address)){
			CompositeAccount accBase = accountSet.register(address, pubKey);
			contractAcc = new ContractAccount(accBase, cache);
			contractAcc.setChaincode(chaincode, -1);
		}else {
			//exist the address;
			long curVersion = accountSet.getVersion(address);
			contractAcc = this.getAccount(address,curVersion);
			contractAcc.setChaincode(chaincode,curVersion);
		}
		contractAcc.setLang(lang);
		return contractAcc;
	}

	/**
	 * 更新指定账户的链码；
	 * 
	 * @param address   合约账户地址；
	 * @param chaincode 链码内容；
	 * @param version   链码版本；
	 * @param lang   合约语言；
	 * @return 返回链码的新版本号；
	 */
	public long update(Bytes address, byte[] chaincode, long version, ContractLang lang) {
		CompositeAccount accBase = accountSet.getAccount(address);
		ContractAccount contractAcc = new ContractAccount(accBase, cache);
		contractAcc.setLang(lang);
		return contractAcc.setChaincode(chaincode, version);
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
		if(accountSet.isUpdated()) {
			accountSet.cancel();
			cache.clear();
		}
	}

	public void setState(Bytes address, AccountState state) {
		getAccount(address).setState(state);
	}

	// used only by kv type ledger structure, clear accountset dataset cache index
	public void clearCachedIndex() {
		accountSet.clearCachedIndex();
	}

	// used only by kv type ledger structure, update preblockheight after block commit
	public void updatePreBlockHeight(long newBlockHeight) {
		accountSet.updatePreBlockHeight(newBlockHeight);
	}
}