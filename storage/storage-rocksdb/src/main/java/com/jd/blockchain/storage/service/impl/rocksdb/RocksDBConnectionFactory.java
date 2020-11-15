package com.jd.blockchain.storage.service.impl.rocksdb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.rocksdb.*;

import com.jd.blockchain.storage.service.DbConnection;
import com.jd.blockchain.storage.service.DbConnectionFactory;
import org.rocksdb.util.SizeUnit;

public class RocksDBConnectionFactory implements DbConnectionFactory {

	static {
		RocksDB.loadLibrary();
	}

	public static final String URI_SCHEME = "rocksdb";

	public static final Pattern URI_PATTER = Pattern
			.compile("^\\w+\\://(/)?\\w+(\\:)?([/\\\\].*)*$");

	private Map<String, RocksDBConnection> connections = new ConcurrentHashMap<>();

	@Override
	public DbConnection connect(String dbUri) {
		return connect(dbUri, null);
	}

	@Override
	public synchronized DbConnection connect(String dbConnectionString, String password) {
		if (!URI_PATTER.matcher(dbConnectionString).matches()) {
			throw new IllegalArgumentException("Illegal format of rocksdb connection string!");
		}
		URI dbUri = URI.create(dbConnectionString.replace("\\", "/"));
		if (!support(dbUri.getScheme())) {
			throw new IllegalArgumentException(
					String.format("Not supported db connection string with scheme \"%s\"!", dbUri.getScheme()));
		}

		String uriHead = dbPrefix();
		int beginIndex = dbConnectionString.indexOf(uriHead);
		String dbPath = dbConnectionString.substring(beginIndex + uriHead.length());

		RocksDBConnection conn = connections.get(dbPath);
		if (conn != null) {
			return conn;
		}

		Options options = initOptions();

		conn = new RocksDBConnection(dbPath, options);
		connections.put(dbPath, conn);

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
		RocksDBConnection[] conns = connections.values().toArray(new RocksDBConnection[connections.size()]);
		connections.clear();
		for (RocksDBConnection conn : conns) {
			conn.dbClose();
		}
	}

	private Options initOptions() {
		Cache cache = new LRUCache(1024 * SizeUnit.MB, 64, false);
		final BlockBasedTableConfig tableOptions = new BlockBasedTableConfig()
				.setBlockCache(cache)
				.setCacheIndexAndFilterBlocks(true)
				.setCacheIndexAndFilterBlocksWithHighPriority(true)
				.setIndexType(IndexType.kTwoLevelIndexSearch) // 打开分片索引
				.setPartitionFilters(true) // 打开分片过滤器
				.setMetadataBlockSize(4096) // 索引分片的块大小
				.setPinL0FilterAndIndexBlocksInCache(true)
				.setPinTopLevelIndexAndFilter(true)
				.setFilterPolicy(null) // 不设置布隆过滤器
				.setDataBlockIndexType(DataBlockIndexType.kDataBlockBinaryAndHash)
				.setDataBlockHashTableUtilRatio(0.75);
		Options options = new Options()
				.setMaxOpenFiles(1000) // 控制最大打开文件数量，防止内存持续增加
				.setMemTableConfig(new HashLinkedListMemTableConfig())
				.setCompressionType(CompressionType.LZ4_COMPRESSION)
				.setAllowConcurrentMemtableWrite(false)
				.setCreateIfMissing(true)
				.setTableFormatConfig(tableOptions);
		return options;
	}

}
