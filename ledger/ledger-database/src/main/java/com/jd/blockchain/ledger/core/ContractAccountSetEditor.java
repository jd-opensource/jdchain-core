package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.Transactional;

public class ContractAccountSetEditor implements Transactional, ContractAccountSet {

	private MerkleAccountSetEditor accountSet;

	public ContractAccountSetEditor(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage,
			VersioningKVStorage verStorage, AccountAccessPolicy accessPolicy) {
		accountSet = new MerkleAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, accessPolicy);
	}

	public ContractAccountSetEditor(HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
			ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly,
			AccountAccessPolicy accessPolicy) {
		accountSet = new MerkleAccountSetEditor(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
				readonly, accessPolicy);
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
		return new ContractAccount(accBase);
	}

	@Override
	public ContractAccount getAccount(String address) {
		return getAccount(Bytes.fromBase58(address));
	}

	@Override
	public ContractAccount getAccount(Bytes address, long version) {
		CompositeAccount accBase = accountSet.getAccount(address, version);
		return new ContractAccount(accBase);
	}

	/**
	 * 部署一项新的合约链码；
	 * 
	 * @param address          合约账户地址；
	 * @param pubKey           合约账户公钥；
	 * @param addressSignature 地址签名；合约账户的私钥对地址的签名；
	 * @param chaincode        链码内容；
	 * @return 合约账户；
	 */
	public ContractAccount deploy(Bytes address, PubKey pubKey, DigitalSignature addressSignature, byte[] chaincode) {
		// TODO: 校验和记录合约地址签名；
		//is exist address?
		ContractAccount contractAcc;
		if(!accountSet.contains(address)){
			CompositeAccount accBase = accountSet.register(address, pubKey);
			contractAcc = new ContractAccount(accBase);
			contractAcc.setChaincode(chaincode, -1);
		}else {
			//exist the address;
			long curVersion = accountSet.getVersion(address);
			contractAcc = this.getAccount(address,curVersion);
			contractAcc.setChaincode(chaincode,curVersion);
		}
		return contractAcc;
	}

	/**
	 * 更新指定账户的链码；
	 * 
	 * @param address   合约账户地址；
	 * @param chaincode 链码内容；
	 * @param version   链码版本；
	 * @return 返回链码的新版本号；
	 */
	public long update(Bytes address, byte[] chaincode, long version) {
		CompositeAccount accBase = accountSet.getAccount(address);
		ContractAccount contractAcc = new ContractAccount(accBase);
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
		accountSet.cancel();
	}

}