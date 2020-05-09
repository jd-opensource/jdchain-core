package com.jd.blockchain.kvdb.server.config;

/**
 * 数据库实例配置
 */
public class DBInfo {
    // 数据库名称
    private String name;
    // 是否开启服务
    private boolean enable;
    // 根目录
    private String dbRootdir;
    // 分片数
    private int partitions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getDbRootdir() {
        return dbRootdir;
    }

    public void setDbRootdir(String dbRootdir) {
        this.dbRootdir = dbRootdir;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }
}
