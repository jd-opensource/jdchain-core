package com.jd.blockchain.storage.service.impl.kvdb;

import com.jd.blockchain.kvdb.client.KVDBClient;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.KVStorageService;
import com.jd.blockchain.storage.service.VersioningKVStorage;

public class KVDBStorageService implements KVStorageService {

    private ExPolicyKVStorage exStorage;

    private VersioningKVStorage verStorage;

    public KVDBStorageService(KVDBClient vDB, KVDBClient eDB) {
        this.verStorage = new KVDBVersioningStorage(vDB);
        this.exStorage = new KVDBExPolicyStorage(eDB);
    }

    @Override
    public ExPolicyKVStorage getExPolicyKVStorage() {
        return exStorage;
    }

    @Override
    public VersioningKVStorage getVersioningKVStorage() {
        return verStorage;
    }

}
