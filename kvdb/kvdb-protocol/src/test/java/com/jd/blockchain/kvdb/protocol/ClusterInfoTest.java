package com.jd.blockchain.kvdb.protocol;

import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBClusterItem;
import org.junit.Assert;
import org.junit.Test;

public class ClusterInfoTest {

    @Test
    public void testMatch() {
        ClusterInfoCase[] cases = new ClusterInfoCase[]{
                // 至少一方为空
                new ClusterInfoCase(
                        new KVDBClusterInfo(new KVDBClusterItem[]{}),
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"})}),
                        new KVDBURI("kvdb://localhost:7079/test1"), 7078, false),
                // 一致
                new ClusterInfoCase(
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"})
                        }),
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"})}),
                        new KVDBURI("kvdb://localhost:7079/test1"), 7078, true),
                // 远端不包含本地
                new ClusterInfoCase(
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"})
                        }),
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7079/test1", "kvdb://localhost:7080/test1"})
                        }),
                        new KVDBURI("kvdb://localhost:7079/test1"), 7078, false),
                // 双方集群数量不同
                new ClusterInfoCase(
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"})
                        }),
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"}),
                                new KVDBClusterItem("test2", new String[]{"kvdb://localhost:7080/test1", "kvdb://localhost:7081/test1"}),
                        }),
                        new KVDBURI("kvdb://localhost:7079/test1"), 7078, true),
                new ClusterInfoCase(
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"}),
                                new KVDBClusterItem("test2", new String[]{"kvdb://localhost:7060/test1", "kvdb://localhost:7061/test1"})
                        }),
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"}),
                                new KVDBClusterItem("test2", new String[]{"kvdb://localhost:7080/test1", "kvdb://localhost:7081/test1"}),
                        }),
                        new KVDBURI("kvdb://localhost:7079/test1"), 7078, true),
                // 远端包含本地但不在同一集群中
                new ClusterInfoCase(
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"}),
                                new KVDBClusterItem("test2", new String[]{"kvdb://localhost:7060/test1", "kvdb://localhost:7061/test1"})
                        }),
                        new KVDBClusterInfo(new KVDBClusterItem[]{
                                new KVDBClusterItem("test2", new String[]{"kvdb://localhost:7078/test1", "kvdb://localhost:7079/test1"}),
                                new KVDBClusterItem("test1", new String[]{"kvdb://localhost:7080/test1", "kvdb://localhost:7081/test1"}),
                        }),
                        new KVDBURI("kvdb://localhost:7079/test1"), 7078, false),
        };
        for (ClusterInfoCase c : cases) {
            Assert.assertEquals(c.match, c.local.match(c.localHost, c.remoteUri, c.remote));
        }
    }

    class ClusterInfoCase {
        public KVDBClusterInfo local;
        public KVDBClusterInfo remote;
        public KVDBURI remoteUri;
        public int localHost;
        public boolean match;

        public ClusterInfoCase(KVDBClusterInfo local, KVDBClusterInfo remote, KVDBURI remoteUri, int localHost, boolean match) {
            this.local = local;
            this.remote = remote;
            this.remoteUri = remoteUri;
            this.localHost = localHost;
            this.match = match;
        }


    }

}
