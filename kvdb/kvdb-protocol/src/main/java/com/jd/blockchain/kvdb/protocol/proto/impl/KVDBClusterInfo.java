package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.KVDBURI;
import com.jd.blockchain.kvdb.protocol.proto.ClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.ClusterItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KVDBClusterInfo implements ClusterInfo {

    private ClusterItem[] clusterItems;

    public KVDBClusterInfo(ClusterItem[] clusterItems) {
        this.clusterItems = clusterItems;
    }

    @Override
    public int size() {
        return null != clusterItems ? clusterItems.length : 0;
    }

    @Override
    public ClusterItem[] getClusterItems() {
        return clusterItems;
    }

    public void setClusterItems(ClusterItem[] clusterItems) {
        this.clusterItems = clusterItems;
    }

    /**
     * 匹配远端集群配置
     *
     * @param localPort   本地端口
     * @param uri         远端URI
     * @param clusterInfo 远端集群配置
     * @return
     */
    @Override
    public boolean match(int localPort, KVDBURI uri, ClusterInfo clusterInfo) {
        // 远端集群配置项不能为空
        if (null == clusterInfo || clusterInfo.size() == 0) {
            return false;
        }
        // 本地包含localhost和远端地址的配置项
        Map<String, String[]> localMapping = new HashMap<>();
        for (int i = 0; i < size(); i++) {
            boolean containLocal = false;
            boolean containRemote = false;
            for (String url : getClusterItems()[i].getURLs()) {
                KVDBURI kvdburi = new KVDBURI(url);
                if (kvdburi.isLocalhost() && kvdburi.getPort() == localPort) {
                    containLocal = true;
                }
                if (kvdburi.getHost().equals(uri.getHost()) && kvdburi.getPort() == uri.getPort()) {
                    containRemote = true;
                }
                if (containLocal && containRemote) {
                    break;
                }
            }
            if (containLocal && containRemote) {
                localMapping.put(getClusterItems()[i].getName(), getClusterItems()[i].getURLs());
            }
        }
        // 远端包含localhost和远端地址的配置项
        Map<String, String[]> remoteMapping = new HashMap<>();
        for (int i = 0; i < clusterInfo.size(); i++) {
            boolean containLocal = false;
            boolean containRemote = false;
            for (String url : clusterInfo.getClusterItems()[i].getURLs()) {
                KVDBURI kvdburi = new KVDBURI(url);
                if (kvdburi.isLocalhost() && kvdburi.getPort() == localPort) {
                    containLocal = true;
                }
                if (kvdburi.getHost().equals(uri.getHost()) && kvdburi.getPort() == uri.getPort()) {
                    containRemote = true;
                }
                if (containLocal && containRemote) {
                    break;
                }
            }
            if (containLocal && containRemote) {
                remoteMapping.put(clusterInfo.getClusterItems()[i].getName(), clusterInfo.getClusterItems()[i].getURLs());
            }
        }
        // 配置项数量一致
        if (localMapping.size() != remoteMapping.size()) {
            return false;
        }
        // 配置项一致
        for (Map.Entry<String, String[]> entry : localMapping.entrySet()) {
            String[] localUrls = entry.getValue();
            String[] remoteUrls = remoteMapping.get(entry.getKey());
            if (null == remoteUrls || remoteUrls.length == 0) {
                return false;
            }
            Arrays.sort(localUrls);
            Arrays.sort(remoteUrls);
            for (int i = 0; i < localUrls.length; i++) {
                KVDBURI localUri = new KVDBURI(localUrls[i]);
                KVDBURI remoteUri = new KVDBURI(remoteUrls[i]);
                if (!localUri.getHost().equals(remoteUri.getHost()) || localUri.getPort() != remoteUri.getPort()) {
                    return false;
                }
            }
        }

        return true;
    }

}
