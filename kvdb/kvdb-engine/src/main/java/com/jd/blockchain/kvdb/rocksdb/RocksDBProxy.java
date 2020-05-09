package com.jd.blockchain.kvdb.rocksdb;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.KVWriteBatch;
import com.jd.blockchain.utils.io.BytesUtils;
import com.jd.blockchain.utils.io.FileUtils;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RocksDBProxy extends KVDBInstance {

    private static Logger LOGGER = LoggerFactory.getLogger(RocksDBProxy.class);

    private WriteOptions writeOptions;

    private RocksDB db;

    protected String path;

    public String getPath() {
        return path;
    }

    private RocksDBProxy(RocksDB db, String path) {
        this.db = db;
        this.path = path;
        this.writeOptions = initWriteOptions();

    }

    private static WriteOptions initWriteOptions() {
        WriteOptions options = new WriteOptions();
        options.setDisableWAL(false);
        options.setNoSlowdown(false);
        return options;
    }

    private static Options initDBOptions() {
        final Filter bloomFilter = new BloomFilter(32);
        final BlockBasedTableConfig tableOptions = new BlockBasedTableConfig()
                .setFilter(bloomFilter)
                .setBlockSize(4 * SizeUnit.KB)
                .setBlockSizeDeviation(10)
                .setBlockCacheSize(128 * SizeUnit.GB)
                .setNoBlockCache(false)
                .setCacheIndexAndFilterBlocks(true)
                .setBlockRestartInterval(16);
        final List<CompressionType> compressionLevels = new ArrayList<>();
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 0-1
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 1-2
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 2-3
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 3-4
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 4-5
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 5-6
        compressionLevels.add(CompressionType.NO_COMPRESSION); // 6-7

        Options options = new Options()
                .setAllowConcurrentMemtableWrite(true)
                .setEnableWriteThreadAdaptiveYield(true)
                .setCreateIfMissing(true)
                .setMaxWriteBufferNumber(3)
                .setTableFormatConfig(tableOptions)
                .setMaxBackgroundCompactions(10)
                .setMaxBackgroundFlushes(4)
                .setBloomLocality(10)
                .setMinWriteBufferNumberToMerge(4)
                .setCompressionPerLevel(compressionLevels)
                .setNumLevels(7)
                .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
                .setCompactionStyle(CompactionStyle.UNIVERSAL)
                .setMemTableConfig(new SkipListMemTableConfig());
        return options;
    }

    private static MutableColumnFamilyOptions initColumnFamilyOptions() {
        return MutableColumnFamilyOptions.builder()
                .setWriteBufferSize(32 * 1024 * 1024)
                .setMaxWriteBufferNumber(4)
                .build();
    }

    private static void initDB(RocksDB db) throws RocksDBException {
        ColumnFamilyHandle defaultColumnFamily = db.getDefaultColumnFamily();
        db.setOptions(defaultColumnFamily, initColumnFamilyOptions());
    }

    public static RocksDBProxy open(String path) throws RocksDBException {
        RocksDB db = RocksDB.open(initDBOptions(), path);

        initDB(db);

        return new RocksDBProxy(db, path);
    }

    @Override
    public byte[] get(byte[] key) throws RocksDBException {
        return db.get(key);
    }

    @Override
    public KVWriteBatch beginBatch() {
        return new RocksDBWriteBatch(db);
    }

    @Override
    public void set(byte[] key, byte[] value) throws RocksDBException {
        db.put(writeOptions, key, value);
    }

    private static class RocksDBWriteBatch implements KVWriteBatch {

        private RocksDB db;
        private WriteBatch batch;

        public RocksDBWriteBatch(RocksDB db) {
            this.db = db;
            this.batch = new WriteBatch();
        }

        @Override
        public void set(String key, String value) throws RocksDBException {
            set(BytesUtils.toBytes(key), BytesUtils.toBytes(value));
        }

        @Override
        public void set(byte[] key, byte[] value) throws RocksDBException {
            batch.put(key, value);
        }

        @Override
        public void commit() throws RocksDBException {
            db.write(new WriteOptions(), batch);
        }

    }

    @Override
    public void close() {
        if (db != null) {
            try {
                db.close();
            } catch (Exception e) {
                LOGGER.error("Error occurred while closing rocksdb[" + path + "]", e);
            } finally {
                db = null;
            }
        }
    }

    @Override
    public synchronized void drop() {
        if (db != null) {
            try {
                close();
                FileUtils.deleteFile(path);
            } catch (Exception e) {
                LOGGER.error("Error occurred while dropping rocksdb[" + path + "]", e);
            }
        }
    }

}
