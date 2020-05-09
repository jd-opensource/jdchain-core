package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.proto.ClusterItem;

/**
 * Cluster information
 */
public class KVDBClusterItem implements ClusterItem {

    // cluster name
    private String name;
    // Cluster URLs
    private String[] urls;

    public KVDBClusterItem(String name, String[] urls) {
        this.name = name;
        this.urls = urls;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String[] getURLs() {
        return urls;
    }

    public void setURLs(String[] urls) {
        this.urls = urls;
    }
}
