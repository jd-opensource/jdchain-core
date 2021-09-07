package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import utils.Bytes;
import utils.SkippingIterator;
import utils.Transactional;

public class EventAccountSetEditorSimple implements EventAccountSet, Transactional {
    static {
        DataContractRegistry.register(Event.class);
    }

    private SimpleAccountSetEditor accountSet;

    private static final String EVENTACCOUNTSET_PREFIX = "EVENTACCOUNTSET" + LedgerConsts.KEY_SEPERATOR;

    public EventAccountSetEditorSimple(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage,
                                       VersioningKVStorage verStorage, AccountAccessPolicy accessPolicy) {
        accountSet = new SimpleAccountSetEditor(cryptoSetting, Bytes.fromString(prefix + EVENTACCOUNTSET_PREFIX), exStorage, verStorage, accessPolicy);
    }

    public EventAccountSetEditorSimple(HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
                                       ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly,
                                       AccountAccessPolicy accessPolicy) {
        accountSet = new SimpleAccountSetEditor(dataRootHash, cryptoSetting, Bytes.fromString(prefix + EVENTACCOUNTSET_PREFIX), exStorage, verStorage,
                readonly, accessPolicy);
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
        return getAccount(address, -1);
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
        accountSet.cancel();
    }

    public DataAccount register(Bytes address, PubKey pubKey, DigitalSignature addressSignature) {
        // TODO: 未实现对地址签名的校验和记录；
        CompositeAccount accBase = accountSet.register(address, pubKey);
        return new DataAccount(accBase);
    }
}
