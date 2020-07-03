package com.jd.blockchain.ump.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author zhaogw
 * @date 2020/6/29 13:46
 */
public class LedgerServiceHandlerTest {
    private LedgerServiceHandler ledgerServiceHandler;

    @Before
    public void setup(){
        ledgerServiceHandler = new LedgerServiceHandler();
    }

    @Test
    public void peerVerifyKey(){
        String userDirPath = System.getProperty("user.dir");
        String peerPath1 = userDirPath + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "peerLib1";
        Assert.assertEquals(peerPath1+File.separator+"system"+File.separator+"deploy-peer.jar",
                ledgerServiceHandler.peerVerifyKey(peerPath1));

        String peerPath2 = userDirPath + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "peerLib2";
        Assert.assertNull(ledgerServiceHandler.peerVerifyKey(peerPath2));
    }
}
