package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.proto.ClusterItem;
import com.jd.blockchain.kvdb.protocol.proto.DatabaseClusterInfo;

/**
 * 数据库实例集群信息
 */
public class KVDBDatabaseClusterInfo implements DatabaseClusterInfo {

    // Whether is cluster mode
    private boolean clusterMode;
    // Cluster info
    private ClusterItem clusterItem;

    @Override
    public boolean isClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }

    @Override
    public ClusterItem getClusterItem() {
        return clusterItem;
    }

    public void setClusterItem(ClusterItem clusterItem) {
        this.clusterItem = clusterItem;
    }

}
