package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.SkippingIterator;
import utils.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DataAccountSetEditor implements Transactional, DataAccountSet {
	private Logger logger = LoggerFactory.getLogger(DataAccountSetEditor.class);

	private BaseAccountSetEditor accountSet;

	public DataAccountSetEditor(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage,
			VersioningKVStorage verStorage, AccountAccessPolicy accessPolicy, LedgerDataStructure dataStructure) {

		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			accountSet = new MerkleAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, accessPolicy);
		} else {
			accountSet = new KvAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, accessPolicy, DatasetType.DATAS);
		}
	}

	public DataAccountSetEditor(long preBlockHeight, HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
									  ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly, LedgerDataStructure dataStructure,
								AccountAccessPolicy accessPolicy) {
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			accountSet = new MerkleAccountSetEditor(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
					readonly, accessPolicy);
		} else {
			accountSet = new KvAccountSetEditor(preBlockHeight, dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
					readonly, accessPolicy, DatasetType.DATAS);
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

	@Override
	public long getTotal() {
		return accountSet.getTotal();
	}

	@Override
	public boolean contains(Bytes address) {
		return accountSet.contains(address);
	}

	/**
	 * 返回账户的存在性证明；
	 */
	@Override
	public MerkleProof getProof(Bytes address) {
		return accountSet.getProof(address);
	}

	public DataAccount register(Bytes address, PubKey pubKey, DigitalSignature addressSignature) {
		// TODO: 未实现对地址签名的校验和记录；
		if(logger.isDebugEnabled()){
			logger.debug("before accountSet.register(),[address={}]",address.toBase58());
		}
		CompositeAccount accBase = accountSet.register(address, pubKey);
		if(logger.isDebugEnabled()){
			logger.debug("after accountSet.register(),[address={}]",address.toBase58());
		}
		return new DataAccount(accBase);
	}

	@Override
	public DataAccount getAccount(String address) {
		return getAccount(Bytes.fromBase58(address));
	}

	/**
	 * 返回数据账户； <br>
	 * 如果不存在，则返回 null；
	 *
	 * @param address
	 * @return
	 */
	@Override
	public DataAccount getAccount(Bytes address) {
		CompositeAccount accBase = accountSet.getAccount(address);
		if (accBase == null) {
			return null;
		}
		return new DataAccount(accBase);
	}

	@Override
	public DataAccount getAccount(Bytes address, long version) {
		CompositeAccount accBase = accountSet.getAccount(address, version);
		return new DataAccount(accBase);
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

	// used only by kv type ledger structure, get new add kv nums
	public Map<Bytes, Long> getKvNumCache() {
		return accountSet.getKvNumCache();
	}

	// used only by kv type ledger structure, clear accountset dataset cache index
	public void clearCachedIndex() {
		accountSet.clearCachedIndex();
	}
}
