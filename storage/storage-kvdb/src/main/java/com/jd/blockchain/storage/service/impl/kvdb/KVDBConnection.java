package com.jd.blockchain.storage.service.impl.kvdb;

import com.jd.blockchain.kvdb.client.KVDBClient;
import com.jd.blockchain.kvdb.protocol.KVDBURI;
import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import com.jd.blockchain.storage.service.DbConnection;
import com.jd.blockchain.storage.service.KVStorageService;

public class KVDBConnection implements DbConnection {

    private KVDBStorageService storage;
    private KVDBClient eDB;
    private KVDBClient vDB;

    public KVDBConnection(KVDBURI dbUri) {
        ClientConfig config = new ClientConfig(dbUri.getHost(), dbUri.getPort(), dbUri.getDatabase());
        eDB = new KVDBClient(config);
        vDB = new KVDBClient(config);
        this.storage = new KVDBStorageService(vDB, eDB);
    }

    @Override
    public void close() {
        eDB.close();
        vDB.close();
    }

    @Override
    public KVStorageService getStorageService() {
        return storage;
    }

}
