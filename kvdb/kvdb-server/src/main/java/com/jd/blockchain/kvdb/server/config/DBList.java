package com.jd.blockchain.kvdb.server.config;

import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

/**
 * 数据库配置，已开启服务的数据库会在kvdb-server启动后自动创建或加载
 */
public class DBList {

    private static final String PROPERTITY_PREFIX = "db";
    private static final String PROPERTITY_SEPARATOR = ".";
    private static final String PROPERTITY_ENABLE = "enable";
    private static final String PROPERTITY_ROOTDIR = "rootdir";
    private static final String PROPERTITY_PARTITIONS = "partitions";

    private String configFile;

    // 数据库名做主键
    private Map<String, DBInfo> dbs = new HashMap<>();

    /**
     * 解析并保存dblist中配置项
     * <p>
     * 校验：
     * 1. 不能存在同名数据库
     *
     * @param configFile
     * @param kvdbConfig
     * @throws IOException
     */
    public DBList(String configFile, KVDBConfig kvdbConfig) throws IOException {
        this.configFile = configFile;
        Properties properties = new Properties();
        properties.load(new FileInputStream(configFile));
        Set<String> dbNames = new HashSet<>();
        for (Object key : properties.keySet()) {
            String[] ps = ((String) key).split("\\.");
            if (ps[2].equals(PROPERTITY_ENABLE)) {
                String dbName = ps[1];
                if (dbNames.contains(dbName)) {
                    throw new KVDBException("duplicate database name : " + dbName);
                }
                dbNames.add(dbName);
            }
        }
        for (String dbName : dbNames) {
            DBInfo config = new DBInfo();
            config.setName(dbName);
            config.setEnable(Boolean.parseBoolean(properties.getProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbName + PROPERTITY_SEPARATOR + PROPERTITY_ENABLE)));
            config.setDbRootdir(properties.getProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbName + PROPERTITY_SEPARATOR + PROPERTITY_ROOTDIR, kvdbConfig.getDbsRootdir()));
            config.setPartitions(Integer.parseInt(properties.getProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbName + PROPERTITY_SEPARATOR + PROPERTITY_PARTITIONS, String.valueOf(kvdbConfig.getDbsPartitions()))));
            dbs.put(dbName, config);
        }
    }

    /**
     * 获取当前服务器已开启服务的所有数据库实例列表，
     *
     * @return
     */
    public List<DBInfo> getEnabledDatabases() {
        List<DBInfo> infos = new ArrayList<>();
        for (DBInfo db : dbs.values()) {
            if (db.isEnable()) {
                infos.add(db);
            }
        }
        return infos;
    }

    /**
     * 获取当前服务器已开启服务的所有数据库实例名称列表，
     *
     * @return
     */
    public Set<String> getEnabledDatabaseNameSet() {
        Set<String> infos = new HashSet<>();
        for (Map.Entry<String, DBInfo> entry : dbs.entrySet()) {
            if (entry.getValue().isEnable()) {
                infos.add(entry.getKey());
            }
        }
        return infos;
    }

    /**
     * 获取当前服务器所有数据库实例名称列表，
     *
     * @return
     */
    public Set<String> getDatabaseNameSet() {
        return dbs.keySet();
    }

    /**
     * 获取当前服务器所有数据库实例列表
     *
     * @return
     */
    public Collection<DBInfo> getDatabases() {
        return dbs.values();
    }

    /**
     * 获取数据库实例
     *
     * @return
     */
    public DBInfo getDatabase(String database) {
        return dbs.get(database);
    }

    /**
     * 保存新创建数据库信息
     *
     * @param dbInfo
     * @throws IOException
     */
    public void createDatabase(DBInfo dbInfo) throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(configFile, true);
            fw.write("\n" + PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbInfo.getName() + PROPERTITY_SEPARATOR + PROPERTITY_ENABLE + "=true");
            fw.write("\n" + PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbInfo.getName() + PROPERTITY_SEPARATOR + PROPERTITY_ROOTDIR + "=" + dbInfo.getDbRootdir());
            fw.write("\n" + PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbInfo.getName() + PROPERTITY_SEPARATOR + PROPERTITY_PARTITIONS + "=" + dbInfo.getPartitions());

            dbs.put(dbInfo.getName(), dbInfo);

        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (null != fw) {
                    fw.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * 开放数据库实例
     *
     * @param database
     */
    public void enableDatabase(String database) throws ConfigurationException, IOException {
        dbs.get(database).setEnable(true);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        config.setLayout(layout);
        try (FileReader reader = new FileReader(configFile)) {
            layout.load(config, reader);
        }
        config.setProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + database + PROPERTITY_SEPARATOR + PROPERTITY_ENABLE, true);
        try (FileWriter fileWriter = new FileWriter(configFile)) {
            layout.save(config, fileWriter);
        }
    }

    /**
     * 关闭数据库实例
     *
     * @param database
     */
    public void disableDatabase(String database) throws ConfigurationException, IOException {
        dbs.get(database).setEnable(false);
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        config.setLayout(layout);
        try (FileReader reader = new FileReader(configFile)) {
            layout.load(config, reader);
        }
        config.setProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + database + PROPERTITY_SEPARATOR + PROPERTITY_ENABLE, false);
        try (FileWriter fileWriter = new FileWriter(configFile)) {
            layout.save(config, fileWriter);
        }
    }

    /**
     * 删除数据库实例
     *
     * @param dbInfo
     */
    public void dropDatabase(DBInfo dbInfo) throws ConfigurationException, IOException {
        dbs.remove(dbInfo.getName());
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        config.setLayout(layout);
        try (FileReader reader = new FileReader(configFile)) {
            layout.load(config, reader);
        }
        config.clearProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbInfo.getName() + PROPERTITY_SEPARATOR + PROPERTITY_ENABLE);
        config.clearProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbInfo.getName() + PROPERTITY_SEPARATOR + PROPERTITY_ROOTDIR);
        config.clearProperty(PROPERTITY_PREFIX + PROPERTITY_SEPARATOR + dbInfo.getName() + PROPERTITY_SEPARATOR + PROPERTITY_PARTITIONS);
        try (FileWriter fileWriter = new FileWriter(configFile)) {
            layout.save(config, fileWriter);
        }
        FileUtils.forceDelete(new File(dbInfo.getDbRootdir() + File.separator + dbInfo.getName()));
    }
}
