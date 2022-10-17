package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.cache.EventAccountCache;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.SkippingIterator;
import utils.Transactional;

import java.util.Map;

public class EventAccountSetEditor implements EventAccountSet, Transactional {
    static {
        DataContractRegistry.register(Event.class);
    }

    private BaseAccountSetEditor accountSet;
    private EventAccountCache cache;

    public EventAccountSetEditor(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage,
                           VersioningKVStorage verStorage, AccountAccessPolicy accessPolicy, LedgerDataStructure dataStructure, EventAccountCache cache) {
        this.cache = cache;
        if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
            accountSet = new MerkleAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, cache, accessPolicy);
        } else {
            accountSet = new KvAccountSetEditor(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, accessPolicy, DatasetType.EVENTS);
        }
    }

    public EventAccountSetEditor(long preBlockHeight, HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
                                       ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly, LedgerDataStructure dataStructure,
                                 EventAccountCache cache, AccountAccessPolicy accessPolicy) {
        this.cache = cache;
        if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
            accountSet = new MerkleAccountSetEditor(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
                    readonly, cache, accessPolicy);
        } else {
            accountSet = new KvAccountSetEditor(preBlockHeight, dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
                    readonly, accessPolicy, DatasetType.EVENTS);
        }
    }

    @Override
    public long getTotal() {
        return accountSet.getTotal();
    }
    
    @Override
    public SkippingIterator<BlockchainIdentity> identityIterator() {
    	return accountSet.identityIterator();
    }

    @Override
    public boolean contains(Bytes address) {
        return accountSet.contains(address);
    }

    @Override
    public MerkleProof getProof(Bytes address) {
        return accountSet.getProof(address);
    }

    @Override
    public EventPublishingAccount getAccount(String address) {
        return getAccount(Bytes.fromBase58(address));
    }

    @Override
    public EventPublishingAccount getAccount(Bytes address) {
        CompositeAccount account = accountSet.getAccount(address);
        if (null == account) {
            return null;
        }
        return new EventPublishingAccount(account, cache);
    }

    @Override
    public EventPublishingAccount getAccount(Bytes address, long version) {
    	CompositeAccount account = accountSet.getAccount(address, version);
        if (null == account) {
            return null;
        }
        return new EventPublishingAccount(account);
    }

    @Override
    public HashDigest getRootHash() {
        return accountSet.getRootHash();
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

    public EventPublishingAccount register(Bytes address, PubKey pubKey, DigitalSignature addressSignature) {
        // TODO: 未实现对地址签名的校验和记录；
        CompositeAccount accBase = accountSet.register(address, pubKey);
        return new EventPublishingAccount(accBase, cache);
    }

    // used only by kv type ledger structure, if add new account
    public boolean isAddNew() {
        return accountSet.isAddNew();
    }

    // used only by kv type ledger structure, get new add kv nums
    public Map<Bytes, Long> getKvNumCache() {
        return accountSet.getKvNumCache();
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
