package test.com.jd.blockchain.gateway;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.gateway.service.GatewayConsensusClientManager;
import com.jd.blockchain.gateway.service.LedgerPeerConnectionManager;
import com.jd.blockchain.gateway.service.LedgerPeersManager;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.setting.GatewayAuthResponse;
import com.jd.blockchain.setting.LedgerIncomingSettings;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import utils.codec.Base58Utils;
import utils.net.NetworkAddress;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * 账本-节点拓扑测试
 */
@Ignore("耗时太长，不同机器环境不一定都能通过，测试用例需要重新设计")
public class LedgerPeersManagerTest {

    static AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
            KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                    "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
    static HashDigest ledger = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
    static NetworkAddress[] peerAddresses = new NetworkAddress[]{
            new NetworkAddress("127.0.0.1", 7080),
            new NetworkAddress("127.0.0.1", 7081),
            new NetworkAddress("127.0.0.1", 7082),
            new NetworkAddress("127.0.0.1", 7083),
    };
    static Set<NetworkAddress> topology = new HashSet<>(Arrays.asList(peerAddresses));
    static ConsensusClientManager clientManager = new GatewayConsensusClientManager();

    static LedgerPeerConnectionManager newMockLedgerPeerConnectionManager(HashDigest ledger, NetworkAddress peerAddress) {
        LedgerPeerConnectionManager mConnectionManager = spy(new LedgerPeerConnectionManager(ledger, peerAddress, keyPair, null, clientManager, null));
        doReturn(new HashDigest[]{ledger}).when(mConnectionManager).connect();
        GatewayAuthResponse authResponse = new GatewayAuthResponse();
        LedgerIncomingSettings settings = new LedgerIncomingSettings();
        settings.setLedgerHash(ledger);
        authResponse.setLedgers(new LedgerIncomingSettings[]{
                settings,
        });
        doReturn(authResponse).when(mConnectionManager).auth();
        doReturn(1l).when(mConnectionManager).ping();
        doReturn(false).doReturn(true).when(mConnectionManager).connected();

        return mConnectionManager;
    }

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, NetworkAddress[] peerAddresses) {
        LedgerPeerConnectionManager[] connectionManagers = new LedgerPeerConnectionManager[peerAddresses.length];
        for (int i = 0; i < peerAddresses.length; i++) {
            connectionManagers[i] = newMockLedgerPeerConnectionManager(ledger, peerAddresses[i]);
        }

        return newMockLedgerPeersManager(ledger, connectionManagers);
    }

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, NetworkAddress peerAddress) {
        return newMockLedgerPeersManager(ledger, new NetworkAddress[]{peerAddress});
    }

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, LedgerPeerConnectionManager[] connectionManagers) {
        LedgerPeersManager ledgerPeersManager = new LedgerPeersManager(ledger, connectionManagers, keyPair, null, clientManager, null, null);
        LedgerPeersManager mLedgerPeersManager = spy(ledgerPeersManager);

        return mLedgerPeersManager;
    }

    /**
     * 拓扑结构更新测试
     */
    @Test
    public void testUpdateTopology() throws InterruptedException {
        Set<NetworkAddress> allPeers = new HashSet<>();
        allPeers.addAll(topology);
        LedgerPeersManager ledgerPeersManager = newMockLedgerPeersManager(ledger, peerAddresses[0]);
        doReturn(topology).when(ledgerPeersManager).updateTopology();
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, peer)).when(ledgerPeersManager).newPeerConnectionManager(peer);
        }
        ledgerPeersManager.startTimerTask();

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertTrue(ledgerPeersManager.getPeerAddresses().containsAll(allPeers));

        allPeers.remove(peerAddresses[0]);
        doReturn(allPeers).when(ledgerPeersManager).updateTopology();
        for (NetworkAddress peer : allPeers) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, peer)).when(ledgerPeersManager).newPeerConnectionManager(peer);
        }
        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertTrue(ledgerPeersManager.getPeerAddresses().containsAll(allPeers));

        doReturn(new HashSet<>()).when(ledgerPeersManager).updateTopology();
        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertTrue(ledgerPeersManager.getPeerAddresses().containsAll(allPeers));
    }

    /**
     * 负载均衡测试
     */
    @Test
    public void testLoadBalance() throws InterruptedException {
        LedgerPeerConnectionManager cm1 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[0]);
        LedgerPeerConnectionManager cm2 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[1]);
        LedgerPeerConnectionManager cm3 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[2]);
        LedgerPeerConnectionManager cm4 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[3]);

        LedgerPeersManager ledgerPeersManager = newMockLedgerPeersManager(ledger, new LedgerPeerConnectionManager[]{cm1, cm2, cm3, cm4});
        doReturn(topology).when(ledgerPeersManager).updateTopology();
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, peer)).when(ledgerPeersManager).newPeerConnectionManager(peer);
        }
        ledgerPeersManager.startTimerTask();

        doReturn(-1l).when(cm1).ping();
        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);

        for (int i = 0; i < 10000; i++) {
            ledgerPeersManager.getQueryService();
            ledgerPeersManager.getTransactionService();
        }
        verify(cm1, times(0)).getQueryService();
        verify(cm2, atLeast(1000)).getQueryService();
        verify(cm3, atLeast(1000)).getQueryService();
        verify(cm4, atLeast(1000)).getQueryService();
        verify(cm1, times(0)).getTransactionService();
        verify(cm2, atLeast(1000)).getTransactionService();
        verify(cm3, atLeast(1000)).getTransactionService();
        verify(cm4, atLeast(1000)).getTransactionService();


        doReturn(1l).when(cm1).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL);
        for (int i = 0; i < 10000; i++) {
            ledgerPeersManager.getQueryService();
            ledgerPeersManager.getTransactionService();
        }
        verify(cm1, atLeast(1000)).getQueryService();
        verify(cm1, atLeast(1000)).getTransactionService();
    }

    /**
     * 有效查询服务数量测试
     */
    @Test
    public void testAvailableQueryServiceSize() throws InterruptedException {
        LedgerPeerConnectionManager cm1 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[0]);
        LedgerPeerConnectionManager cm2 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[1]);
        LedgerPeerConnectionManager cm3 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[2]);
        LedgerPeerConnectionManager cm4 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[3]);

        LedgerPeersManager ledgerPeersManager = newMockLedgerPeersManager(ledger, new LedgerPeerConnectionManager[]{cm1, cm2, cm3, cm4});
        doReturn(topology).when(ledgerPeersManager).updateTopology();
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, peer)).when(ledgerPeersManager).newPeerConnectionManager(peer);
        }
        ledgerPeersManager.startTimerTask();

        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                ledgerPeersManager.getQueryService();
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm1).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                ledgerPeersManager.getQueryService();
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm2).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                ledgerPeersManager.getQueryService();
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm3).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                ledgerPeersManager.getQueryService();
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm4).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            ledgerPeersManager.getQueryService();
            Assert.fail("Should never come here");
        } catch (Exception e) {
        }
    }

    /**
     * 有效交易服务数量测试
     */
    @Test
    public void testAvailableTransactionServiceSize() throws InterruptedException {
        LedgerPeerConnectionManager cm1 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[0]);
        LedgerPeerConnectionManager cm2 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[1]);
        LedgerPeerConnectionManager cm3 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[2]);
        LedgerPeerConnectionManager cm4 = newMockLedgerPeerConnectionManager(ledger, peerAddresses[3]);

        LedgerPeersManager ledgerPeersManager = newMockLedgerPeersManager(ledger, new LedgerPeerConnectionManager[]{cm1, cm2, cm3, cm4});
        doReturn(topology).when(ledgerPeersManager).updateTopology();
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, peer)).when(ledgerPeersManager).newPeerConnectionManager(peer);
        }
        ledgerPeersManager.startTimerTask();

        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                ledgerPeersManager.getTransactionService();
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm1).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                ledgerPeersManager.getTransactionService();
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm2).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            ledgerPeersManager.getTransactionService();
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm3).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            ledgerPeersManager.getTransactionService();
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        doReturn(-1l).when(cm4).ping();
        Thread.sleep(LedgerPeerConnectionManager.PING_INTERVAL * 2);
        try {
            ledgerPeersManager.getTransactionService();
            Assert.fail("Should never come here");
        } catch (Exception e) {
        }
    }

}
