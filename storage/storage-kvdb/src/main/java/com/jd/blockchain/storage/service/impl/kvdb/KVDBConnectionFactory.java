package com.jd.blockchain.storage.service.impl.kvdb;

import com.jd.blockchain.kvdb.protocol.KVDBURI;
import com.jd.blockchain.storage.service.DbConnection;
import com.jd.blockchain.storage.service.DbConnectionFactory;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KVDBConnectionFactory implements DbConnectionFactory {

    public static final String URI_SCHEME = "kvdb";

    private Map<String, KVDBConnection> connections = new ConcurrentHashMap<>();

    @Override
    public DbConnection connect(String dbUri) {
        return connect(dbUri, null);
    }

    @Override
    public synchronized DbConnection connect(String dbConnection, String password) {

        KVDBConnection conn = connections.get(dbConnection);
        if (conn != null) {
            return conn;
        }

        conn = new KVDBConnection(new KVDBURI(dbConnection));
        connections.put(dbConnection, conn);

        return conn;
    }


    @Override
    public String dbPrefix() {
        return URI_SCHEME + "://";
    }

    @Override
    public boolean support(String scheme) {
        return URI_SCHEME.equalsIgnoreCase(scheme);
    }

    @PreDestroy
    @Override
    public void close() {
        KVDBConnection[] conns = connections.values().toArray(new KVDBConnection[connections.size()]);
        connections.clear();
        for (KVDBConnection conn : conns) {
            conn.close();
        }
    }

}
