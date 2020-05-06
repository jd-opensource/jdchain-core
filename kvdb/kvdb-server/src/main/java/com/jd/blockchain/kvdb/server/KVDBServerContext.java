package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.protocol.KVDBURI;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.kvdb.protocol.proto.*;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBDatabaseClusterInfo;
import com.jd.blockchain.kvdb.server.config.DBInfo;
import com.jd.blockchain.kvdb.server.config.ServerConfig;
import com.jd.blockchain.kvdb.server.executor.Executor;
import com.jd.blockchain.utils.StringUtils;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 服务端上下文信息
 */
public class KVDBServerContext implements ServerContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBServerContext.class);

    // 当前服务器所有客户端连接
    private final ConcurrentHashMap<String, Session> clients = new ConcurrentHashMap<>();

    // 所有命令操作集合，命令名-操作对象
    private final Map<String, Executor> executors = new HashMap<>();

    // 服务器配置信息
    private final ServerConfig config;

    // 数据库实例集，数据库名-实例
    private final Map<String, KVDBInstance> rocksdbs;
    // 集群配置集，集群名称-配置
    private Map<String, ClusterItem> clusterInfoMapping;
    // 数据库实例-集群配置对照关系，数据库名-集群名
    private Map<String, String> dbClusterMapping;


    public KVDBServerContext(ServerConfig config) throws RocksDBException {
        this.config = config;
        // 创建或加载 dblist 中配置的数据库实例
        rocksdbs = KVDB.initDBs(config.getDbList());
        // 保存集群配置
        clusterInfoMapping = config.getClusterMapping();
        // 保存数据库实例-集群对照关系
        dbClusterMapping = new HashMap<>();
        for (Map.Entry<String, ClusterItem> entry : clusterInfoMapping.entrySet()) {
            for (String url : entry.getValue().getURLs()) {
                KVDBURI uri = new KVDBURI(url);
                dbClusterMapping.put(uri.getDatabase(), entry.getKey());
            }
        }
    }

    public ServerConfig getConfig() {
        return config;
    }

    public int getClients() {
        return clients.size();
    }

    public Executor getExecutor(String command) {
        Executor executor = executors.get(command.toLowerCase());

        return null != executor ? executor : executors.get(Command.CommandType.UNKNOWN.getCommand().toLowerCase());
    }

    @Override
    public Map<String, KVDBInstance> getDatabases() {
        return rocksdbs;
    }

    @Override
    public KVDBInstance getDatabase(String name) {
        return rocksdbs.get(name);
    }

    public synchronized KVDBInstance createDatabase(DBInfo dbInfo) throws KVDBException, RocksDBException, IOException {
        if (rocksdbs.containsKey(dbInfo.getName())) {
            throw new KVDBException("database already exists");
        }
        KVDBInstance kvdbInstance = KVDB.createDB(config.getKvdbConfig(), dbInfo);
        config.getDbList().createDatabase(dbInfo);
        rocksdbs.put(dbInfo.getName(), kvdbInstance);

        return kvdbInstance;
    }

    @Override
    public DatabaseClusterInfo getDatabaseInfo(String database) {
        KVDBDatabaseClusterInfo info = new KVDBDatabaseClusterInfo();
        String cluster = dbClusterMapping.get(database);
        if (StringUtils.isEmpty(cluster)) {
            info.setClusterMode(false);
        } else {
            info.setClusterMode(true);
            info.setClusterItem(clusterInfoMapping.get(cluster));
        }
        return info;
    }

    @Override
    public ClusterInfo getClusterInfo() {
        return config.getClusterInfo();
    }

    @Override
    public synchronized void enableDatabase(String database) throws KVDBException {
        try {
            // 数据库处于可用状态
            if (rocksdbs.containsKey(database)) {
                return;
            }
            DBInfo dbInfo = config.getDbList().getDatabase(database);
            // 数据库不存在
            if (null == dbInfo) {
                throw new KVDBException("database not exists");
            }
            config.getDbList().enableDatabase(database);
            rocksdbs.put(database, KVDB.initDB(dbInfo));
        } catch (Exception e) {
            throw new KVDBException(e.toString());
        }
    }

    @Override
    public synchronized void disableDatabase(String database) throws KVDBException {
        try {
            // 数据库不处于可用状态
            KVDBInstance instance = rocksdbs.get(database);
            if (null == instance) {
                throw new KVDBException("database not exists or disabled");
            }
            // 参与到集群中的数据库实例不可修改
            if (dbClusterMapping.containsKey(database)) {
                throw new KVDBException("database in cluster can not modified");
            }
            rocksdbs.remove(database);
            // 关闭数据库
            instance.close();
            // 更新数据库状态
            config.getDbList().disableDatabase(database);
        } catch (Exception e) {
            throw new KVDBException(e.toString());
        }
    }

    @Override
    public synchronized void dropDatabase(String database) throws KVDBException {
        try {
            // 数据库实例不存在
            DBInfo dbInfo = config.getDbList().getDatabase(database);
            if (null == dbInfo) {
                throw new KVDBException("database not exists");
            }
            // 参与到集群中的数据库实例不可修改
            if (dbClusterMapping.containsKey(database)) {
                throw new KVDBException("database in cluster can not modified");
            }
            KVDBInstance instance = rocksdbs.get(database);
            // 关闭数据库实例
            if (null != instance) {
                instance.close();
            }
            rocksdbs.remove(database);
            // 更新配置并删除数据库目录
            config.getDbList().dropDatabase(dbInfo);
        } catch (Exception e) {
            throw new KVDBException(e.toString());
        }
    }

    public void stop() {
        clients.clear();
        for (KVDBInstance db : rocksdbs.values()) {
            db.close();
        }
    }

    public Session getSession(String sourceKey, Function<String, Session> factory) {
        return clients.computeIfAbsent(sourceKey, key -> factory.apply(key));
    }

    public Session getSession(String key) {
        return clients.get(key);
    }

    /**
     * 移除会话
     *
     * @param sourceKey
     */
    protected void removeSession(String sourceKey) {
        Session session = getSession(sourceKey);
        if (null != session) {
            session.close();
        }
        clients.remove(sourceKey);
    }

    /**
     * 添加命令处理对象
     *
     * @param name
     * @param executor
     */
    protected void addExecutor(String name, Executor executor) {
        executors.put(name.toLowerCase(), executor);
    }

    /**
     * 执行命令
     *
     * @param sourceKey
     * @param message
     */
    public void processCommand(String sourceKey, Message message) {
        Command command = (Command) message.getContent();
        Session session = getSession(sourceKey);
        session.publish(executors.get(command.getName()).execute(new KVDBRequest(this, session, message)));
    }
}
