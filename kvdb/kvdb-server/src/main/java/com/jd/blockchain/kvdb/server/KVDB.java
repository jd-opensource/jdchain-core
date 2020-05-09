package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.rocksdb.RocksDBCluster;
import com.jd.blockchain.kvdb.rocksdb.RocksDBProxy;
import com.jd.blockchain.kvdb.server.config.DBInfo;
import com.jd.blockchain.kvdb.server.config.DBList;
import com.jd.blockchain.kvdb.server.config.KVDBConfig;
import com.jd.blockchain.utils.io.FileUtils;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KVDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDB.class);

    static {
        RocksDB.loadLibrary();
    }

    /**
     * Load default databases from dblist config.
     *
     * @param dbList
     * @return
     * @throws RocksDBException
     */
    public static Map<String, KVDBInstance> initDBs(DBList dbList) throws RocksDBException {
        Map<String, KVDBInstance> dbs = new HashMap<>();

        for (DBInfo config : dbList.getEnabledDatabases()) {
            if (config.isEnable()) {
                String dbPath = config.getDbRootdir() + File.separator + config.getName();
                FileUtils.makeDirectory(dbPath);
                if (config.getPartitions() > 1) {
                    dbs.put(config.getName(), RocksDBCluster.open(dbPath, config.getPartitions()));
                } else {
                    dbs.put(config.getName(), RocksDBProxy.open(dbPath));
                }
            }
        }

        return dbs;
    }

    /**
     * Load default databases from dbInfo.
     *
     * @param config
     * @return
     * @throws RocksDBException
     */
    public static KVDBInstance initDB(DBInfo config) throws RocksDBException {
        Map<String, KVDBInstance> dbs = new HashMap<>();
        String dbPath = config.getDbRootdir() + File.separator + config.getName();
        FileUtils.makeDirectory(dbPath);
        if (config.getPartitions() > 1) {
            return RocksDBCluster.open(dbPath, config.getPartitions());
        } else {
            return dbs.put(config.getName(), RocksDBProxy.open(dbPath));
        }

    }

    /**
     * Create database
     *
     * @param config
     * @param dbInfo
     * @return
     * @throws RocksDBException
     */
    public static KVDBInstance createDB(KVDBConfig config, DBInfo dbInfo) throws RocksDBException {
        KVDBInstance db;
        String dbPath = dbInfo.getDbRootdir() + File.separator + dbInfo.getName();
        FileUtils.makeDirectory(dbPath);
        if (dbInfo.getPartitions() > 1) {
            db = RocksDBCluster.open(dbPath, config.getDbsPartitions());
        } else {
            db = RocksDBProxy.open(dbPath);
        }

        return db;
    }
}
