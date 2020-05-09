package com.jd.blockchain.kvdb.protocol;

import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import org.junit.Assert;
import org.junit.Test;

public class ConfigTest {

    @Test
    public void test() {
        ClientConfig config = new ClientConfig(new String[]{"-db", "test"});
        Assert.assertEquals("localhost", config.getHost());
        Assert.assertEquals(7078, config.getPort());
        Assert.assertEquals(60000, config.getTimeout());
        Assert.assertEquals(1024 * 1024, config.getBufferSize());
        Assert.assertEquals(5, config.getRetryTimes());
        Assert.assertTrue(config.getKeepAlive());
        Assert.assertEquals("test", config.getDatabase());

        config = new ClientConfig(new String[]{
                "-h", "localhost",
                "-p", "6379",
                "-t", "1000",
                "-bs", "1024",
                "-rt", "1",
                "-k", "false",
                "-db", "10"});
        Assert.assertEquals("localhost", config.getHost());
        Assert.assertEquals(6379, config.getPort());
        Assert.assertEquals(1000, config.getTimeout());
        Assert.assertEquals(1024, config.getBufferSize());
        Assert.assertEquals(1, config.getRetryTimes());
        Assert.assertFalse(config.getKeepAlive());
        Assert.assertEquals("10", config.getDatabase());
    }
}
