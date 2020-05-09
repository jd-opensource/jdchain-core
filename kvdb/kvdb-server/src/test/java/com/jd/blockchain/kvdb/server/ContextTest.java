package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.server.config.ServerConfig;
import com.jd.blockchain.kvdb.server.executor.*;
import com.jd.blockchain.utils.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static com.jd.blockchain.kvdb.protocol.proto.Command.CommandType.*;

public class ContextTest {

    private KVDBServerContext context;

    @Before
    public void setUp() throws Exception {
        context = new KVDBServerContext(new ServerConfig(this.getClass().getResource("/context").getFile()));
    }

    @After
    public void tearDown() {
        context.stop();
        FileUtils.deletePath(new File(context.getConfig().getKvdbConfig().getDbsRootdir()), true);
    }

    @Test
    public void testExecutor() {
        context.addExecutor(USE.getCommand(), new UseExecutor());
        context.addExecutor(CREATE_DATABASE.getCommand(), new CreateDatabaseExecutor());
        context.addExecutor(CLUSTER_INFO.getCommand(), new ClusterInfoExecutor());
        context.addExecutor(EXISTS.getCommand(), new ExistsExecutor());
        context.addExecutor(GET.getCommand(), new GetExecutor());
        context.addExecutor(PUT.getCommand(), new PutExecutor());
        context.addExecutor(BATCH_BEGIN.getCommand(), new BatchBeginExecutor());
        context.addExecutor(BATCH_ABORT.getCommand(), new BatchAbortExecutor());
        context.addExecutor(BATCH_COMMIT.getCommand(), new BatchCommitExecutor());
        context.addExecutor(UNKNOWN.getCommand(), new UnknowExecutor());

        Assert.assertTrue(context.getExecutor(USE.getCommand()) instanceof UseExecutor);
        Assert.assertTrue(context.getExecutor(CREATE_DATABASE.getCommand()) instanceof CreateDatabaseExecutor);
        Assert.assertTrue(context.getExecutor(CLUSTER_INFO.getCommand()) instanceof ClusterInfoExecutor);
        Assert.assertTrue(context.getExecutor(EXISTS.getCommand()) instanceof ExistsExecutor);
        Assert.assertTrue(context.getExecutor(GET.getCommand()) instanceof GetExecutor);
        Assert.assertTrue(context.getExecutor(PUT.getCommand()) instanceof PutExecutor);
        Assert.assertTrue(context.getExecutor(BATCH_BEGIN.getCommand()) instanceof BatchBeginExecutor);
        Assert.assertTrue(context.getExecutor(BATCH_ABORT.getCommand()) instanceof BatchAbortExecutor);
        Assert.assertTrue(context.getExecutor(BATCH_COMMIT.getCommand()) instanceof BatchCommitExecutor);
        Assert.assertTrue(context.getExecutor("test unknown") instanceof UnknowExecutor);

    }

    @Test
    public void testSession() {
        String sourceKey = UUID.randomUUID().toString();
        Session session = context.getSession(sourceKey, key -> new KVDBSession(key, null));
        Assert.assertEquals(session, context.getSession(sourceKey));
        context.removeSession(sourceKey);
        Assert.assertNull(context.getSession(sourceKey));
    }

    @Test
    public void testConfig() {
        Assert.assertEquals(2, context.getDatabases().size());
        Assert.assertNotNull(context.getDatabase("test1"));
        Assert.assertNotNull(context.getDatabase("test2"));
        Assert.assertEquals(1, context.getClusterInfo().size());
        Assert.assertTrue(context.getDatabaseInfo("test1").isClusterMode());
        Assert.assertEquals(2, context.getDatabaseInfo("test1").getClusterItem().getURLs().length);
        Assert.assertEquals("test1", context.getDatabaseInfo("test1").getClusterItem().getName());
        Assert.assertFalse(context.getDatabaseInfo("test2").isClusterMode());
    }

}
