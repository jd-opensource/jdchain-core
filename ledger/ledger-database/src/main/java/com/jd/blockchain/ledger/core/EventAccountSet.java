package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;

/**
 * @Author: zhangshuang
 * @Date: 2020/6/2 11:10 AM
 * Version 1.0
 */
public class EventAccountSet implements EventAccountQuery, Transactional {

    private MerkleAccountSet accountSet;

    public EventAccountSet(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage,
                          VersioningKVStorage verStorage, AccountAccessPolicy accessPolicy) {
        accountSet = new MerkleAccountSet(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, accessPolicy);
    }

    public EventAccountSet(HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
                          ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly,
                          AccountAccessPolicy accessPolicy) {
        accountSet = new MerkleAccountSet(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage,
                readonly, accessPolicy);
    }


    @Override
    public long getTotal() {
        return 0;
    }

    @Override
    public BlockchainIdentity[] getHeaders(int fromIndex, int count) {
        return new BlockchainIdentity[0];
    }

    @Override
    public boolean contains(Bytes address) {
        return false;
    }

    @Override
    public MerkleProof getProof(Bytes address) {
        return null;
    }

    @Override
    public EventPublishingAccount getAccount(String address) {
        return null;
    }

    @Override
    public EventPublishingAccount getAccount(Bytes address) {
        return null;
    }

    @Override
    public EventPublishingAccount getAccount(Bytes address, long version) {
        return null;
    }

    @Override
    public HashDigest getRootHash() {
        return null;
    }

    @Override
    public boolean isUpdated() {
        return false;
    }

    @Override
    public void commit() {

    }

    @Override
    public void cancel() {

    }

    void setReadonly() {
        accountSet.setReadonly();
    }
}
