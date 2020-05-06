package com.jd.blockchain.kvdb.server.config;

import com.jd.blockchain.kvdb.protocol.KVDBURI;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * 集群配置
 */
public class ClusterConfig {

    private static final String PROPERTITY_PREFIX = "cluster";
    private static final String PROPERTITY_SEPARATOR = ".";
    private static final String PROPERTITY_PARTITIONS = "partitions";
    // 集群名称为主键，集群节点配置列表为键值
    private Map<String, String[]> cluster = new HashMap<>();

    /**
     * 解析并保存cluster.conf中配置项
     * <p>
     * 校验:
     * 1. 不能配置dblist中不存在的数据库
     * 2. 集群名字不能重复
     * 3. 同一个数据库不能存在多个集群配置中
     *
     * @param configFile
     * @param dbList     已开启服务的数据库实例配置信息
     * @throws IOException
     */
    public ClusterConfig(String configFile, DBList dbList) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(configFile));
        Set<String> clusterNames = new HashSet<>();
        for (Object key : properties.keySet()) {
            String[] item = ((String) key).split("\\.");
            if(item[2].equals(PROPERTITY_PARTITIONS)) {
                if (cluster.containsKey(item[1])) {
                    throw new KVDBException("duplicate cluster name : " + item[1]);
                }
                clusterNames.add(item[1]);
            }
        }
        Set<String> databasesInDblist = dbList.getEnabledDatabaseNameSet();
        Map<String, String> databaseClusterMapping = new HashMap<>();
        for (String clusterName : clusterNames) {
            int partitions = Integer.parseInt(properties.getProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + clusterName + PROPERTITY_SEPARATOR + PROPERTITY_PARTITIONS));
            if (partitions < 2) {
                throw new KVDBException("cluster :  " + clusterName + " partitions must < 2");
            }
            String[] urls = new String[partitions];
            for (int i = 0; i < partitions; i++) {
                urls[i] = properties.getProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + clusterName + PROPERTITY_SEPARATOR + i);
                KVDBURI uri = new KVDBURI(urls[i]);
                if (!databasesInDblist.contains(uri.getDatabase())) {
                    throw new KVDBException("database :  " + uri.getDatabase() + " not created or enabled");
                }
                if (databaseClusterMapping.containsKey(urls[i])) {
                    throw new KVDBException("multiple clusters include database : " + uri.getDatabase());
                }
                databaseClusterMapping.put(urls[i], clusterName);
            }
            cluster.put(clusterName, urls);
        }
    }

    /**
     * 返回集群配置信息
     *
     * @return
     */
    public Map<String, String[]> getCluster() {
        return cluster;
    }
}
