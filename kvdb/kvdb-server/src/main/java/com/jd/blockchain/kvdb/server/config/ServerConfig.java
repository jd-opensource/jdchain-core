package com.jd.blockchain.kvdb.server.config;

import com.jd.blockchain.kvdb.protocol.proto.ClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.ClusterItem;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBClusterItem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务器配置，聚合cluster.conf/dblist/kvdb.conf
 */
public class ServerConfig {

    private static final String CONFIG_DIR = "config";
    private static final String SYSTEM_DIR = "system";
    private static final String KVDB_CONFIG = "kvdb.conf";
    private static final String CLUSTER_CONFIG = "cluster.conf";
    private static final String DBLIST = "dblist";

    private String kvdbConfigFile;
    private String clusterConfigFile;
    private String dblistFile;

    private KVDBConfig kvdbConfig;
    private ClusterConfig clusterConfig;
    private DBList dbList;

    public ServerConfig(String home) throws IOException {
        File file = new File(home);
        kvdbConfigFile = file.getAbsolutePath() + File.separator + CONFIG_DIR + File.separator + KVDB_CONFIG;
        clusterConfigFile = file.getAbsolutePath() + File.separator + CONFIG_DIR + File.separator + CLUSTER_CONFIG;
        dblistFile = file.getAbsolutePath() + File.separator + SYSTEM_DIR + File.separator + DBLIST;
        kvdbConfig = new KVDBConfig(kvdbConfigFile);
        dbList = new DBList(dblistFile, kvdbConfig);
        clusterConfig = new ClusterConfig(clusterConfigFile, dbList);
    }

    /**
     * @return 集群配置信息，集群名称-集群配置项信息
     */
    public Map<String, ClusterItem> getClusterMapping() {
        Map<String, ClusterItem> clusterMapping = new HashMap<>();
        for (Map.Entry<String, String[]> cluster : clusterConfig.getCluster().entrySet()) {
            clusterMapping.put(cluster.getKey(), new KVDBClusterItem(cluster.getKey(), cluster.getValue()));
        }

        return clusterMapping;
    }

    /**
     * @return 集群配置信息，所有集群配置列表
     */
    public ClusterInfo getClusterInfo() {
        Map<String, String[]> clusterMapping = clusterConfig.getCluster();
        ClusterItem[] clusterItems = new ClusterItem[clusterMapping.size()];
        int i = 0;
        for (Map.Entry<String, String[]> cluster : clusterMapping.entrySet()) {
            clusterItems[i] = new KVDBClusterItem(cluster.getKey(), cluster.getValue());
            i++;
        }

        return new KVDBClusterInfo(clusterItems);
    }

    /**
     * @return 服务配置
     */
    public KVDBConfig getKvdbConfig() {
        return kvdbConfig;
    }

    /**
     * @return 集群配置
     */
    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    /**
     * @return 数据库实例配置
     */
    public DBList getDbList() {
        return dbList;
    }

    public String getKvdbConfigFile() {
        return kvdbConfigFile;
    }

    public String getClusterConfigFile() {
        return clusterConfigFile;
    }

    public String getDblistFile() {
        return dblistFile;
    }
}
