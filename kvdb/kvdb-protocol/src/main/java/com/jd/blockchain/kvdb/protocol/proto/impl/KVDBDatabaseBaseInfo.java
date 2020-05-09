package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.proto.DatabaseBaseInfo;

public class KVDBDatabaseBaseInfo implements DatabaseBaseInfo {

    // 数据库名称
    private String name;
    // 是否开启服务
    private boolean enable;
    // 根目录
    private String rootDir;
    // 分片数
    private int partitions;

    public KVDBDatabaseBaseInfo(String name, String rootDir, Integer partitions) {
        this(name, rootDir, partitions, true);
    }

    public KVDBDatabaseBaseInfo(String name) {
        this(name, "", 0, true);
    }

    public KVDBDatabaseBaseInfo(String name, String rootDir, Integer partitions, boolean enable) {
        this.name = name;
        this.rootDir = rootDir;
        this.partitions = partitions;
        this.enable = enable;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
