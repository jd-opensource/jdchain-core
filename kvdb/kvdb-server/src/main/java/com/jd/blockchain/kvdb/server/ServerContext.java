package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.protocol.proto.ClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.DatabaseClusterInfo;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.kvdb.server.config.DBInfo;
import com.jd.blockchain.kvdb.server.config.ServerConfig;
import com.jd.blockchain.kvdb.server.executor.Executor;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.Map;

/**
 * 服务器上下文信息
 */
public interface ServerContext {

    /**
     * @return 配置
     */
    ServerConfig getConfig();

    /**
     * @param sourceKey 连接唯一标识
     * @return 连接会话
     */
    Session getSession(String sourceKey);

    /**
     * @param command 命令名称
     * @return 命令对象者
     */
    Executor getExecutor(String command);

    /**
     * @return 数据库实例集合，数据库名-数据库实例
     */
    Map<String, KVDBInstance> getDatabases();

    /**
     * @param name 数据库名称
     * @return 数据库实例
     */
    KVDBInstance getDatabase(String name);

    /**
     * 创建数据库
     *
     * @param dbInfo 数据实例信息
     * @return 数据库实例
     * @throws KVDBException
     * @throws RocksDBException
     * @throws IOException
     */
    KVDBInstance createDatabase(DBInfo dbInfo) throws KVDBException, RocksDBException, IOException;

    /**
     * @param database 数据库名
     * @return 数据库信息
     */
    DatabaseClusterInfo getDatabaseInfo(String database);

    /**
     * @return 集群信息
     */
    ClusterInfo getClusterInfo();

    /**
     * 开放数据库实例
     *
     * @param database
     */
    void enableDatabase(String database) throws KVDBException, ConfigurationException;

    /**
     * 关闭数据库实例
     *
     * @param database
     */
    void disableDatabase(String database) throws KVDBException, ConfigurationException;

    /**
     * 删除数据库实例
     *
     * @param database
     */
    void dropDatabase(String database) throws KVDBException;
}
