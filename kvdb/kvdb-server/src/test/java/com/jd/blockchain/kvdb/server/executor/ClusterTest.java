package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.kvdb.protocol.Constants;
import com.jd.blockchain.kvdb.protocol.proto.ClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.DatabaseClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.Response;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.KVDBRequest;
import com.jd.blockchain.kvdb.server.KVDBServerContext;
import com.jd.blockchain.kvdb.server.KVDBSession;
import com.jd.blockchain.kvdb.server.Session;
import com.jd.blockchain.kvdb.server.config.ServerConfig;
import com.jd.blockchain.utils.io.FileUtils;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

public class ClusterTest {

    private KVDBServerContext context;
    // 用于dblist恢复
    private PropertiesConfiguration config;
    private PropertiesConfigurationLayout layout;

    @Before
    public void setUp() throws Exception {
        context = new KVDBServerContext(new ServerConfig(this.getClass().getResource("/executor/cluster").getFile()));
        config = new PropertiesConfiguration();
        layout = new PropertiesConfigurationLayout();
        config.setLayout(layout);
        try (FileReader reader = new FileReader(context.getConfig().getDblistFile())) {
            layout.load(config, reader);
        }
    }

    @After
    public void tearDown() throws Exception {
        context.stop();
        FileUtils.deletePath(new File(context.getConfig().getKvdbConfig().getDbsRootdir()), true);
        try (FileWriter fileWriter = new FileWriter(context.getConfig().getDblistFile())) {
            layout.save(config, fileWriter);
        }
    }

    private Session newSession() {
        return context.getSession(UUID.randomUUID().toString(), key -> new KVDBSession(key, null));
    }

    private Session newSessionWithTestDB() throws RocksDBException {
        Session session = context.getSession(UUID.randomUUID().toString(), key -> new KVDBSession(key, null));

        session.setDB("test1", context.getDatabase("test1"));

        return session;
    }

    private Response execute(Session session, Executor executor, Message message) {
        return (Response) executor.execute(new KVDBRequest(context, session, message)).getContent();
    }

    @Test
    public void testUse() {
        Session session = newSession();

        Assert.assertNull(session.getDBInstance());

        Response response = execute(session, new UseExecutor(), KVDBMessage.use("test1"));
        Assert.assertEquals(Constants.SUCCESS, response.getCode());
        DatabaseClusterInfo clusterInfo = BinaryProtocol.decodeAs(response.getResult()[0].toBytes(), DatabaseClusterInfo.class);
        Assert.assertTrue(clusterInfo.isClusterMode());
        Assert.assertEquals("test1", clusterInfo.getClusterItem().getName());
        Assert.assertEquals(2, clusterInfo.getClusterItem().getURLs().length);

        Assert.assertNotNull(session.getDBInstance());
    }

    @Test
    public void testEnableDB() {
        Session session = newSession();

        Response response = execute(session, new EnableDatabaseExecutor(), KVDBMessage.enableDatabase("test1"));
        Assert.assertEquals(Constants.SUCCESS, response.getCode());
    }

    @Test
    public void testDisableDB() {
        Session session = newSession();

        Response response = execute(session, new DisableDatabaseExecutor(), KVDBMessage.disableDatabase("test1"));
        Assert.assertEquals(Constants.ERROR, response.getCode());
    }

    @Test
    public void testDropDB() {
        Session session = newSession();

        Response response = execute(session, new DropDatabaseExecutor(), KVDBMessage.dropDatabase("test1"));
        Assert.assertEquals(Constants.ERROR, response.getCode());
    }

    @Test
    public void testClusterInfo() throws RocksDBException {
        Session session = newSessionWithTestDB();

        Response response = execute(session, new ClusterInfoExecutor(), KVDBMessage.clusterInfo());
        Assert.assertEquals(Constants.SUCCESS, response.getCode());
        ClusterInfo info = BinaryProtocol.decodeAs(response.getResult()[0].toBytes(), ClusterInfo.class);
        Assert.assertNotNull(info);
        Assert.assertEquals(2, info.size());
    }

}
