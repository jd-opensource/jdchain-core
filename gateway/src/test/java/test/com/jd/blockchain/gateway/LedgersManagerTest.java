package test.com.jd.blockchain.gateway;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.gateway.service.GatewayConsensusClientManager;
import com.jd.blockchain.gateway.service.LedgerPeerConnectionListener;
import com.jd.blockchain.gateway.service.LedgerPeerConnectionManager;
import com.jd.blockchain.gateway.service.LedgerPeersManager;
import com.jd.blockchain.gateway.service.LedgersListener;
import com.jd.blockchain.gateway.service.LedgersManager;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.setting.GatewayAuthResponse;
import com.jd.blockchain.setting.LedgerIncomingSettings;
import org.junit.Assert;
import org.junit.Test;
import utils.codec.Base58Utils;
import utils.net.NetworkAddress;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

public class LedgersManagerTest {

    static ConsensusClientManager clientManager = new GatewayConsensusClientManager();

    static LedgerPeerConnectionManager newMockLedgerPeerConnectionManager(HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress peerAddress, LedgersListener ledgersListener) {
        return newMockLedgerPeerConnectionManager(ledger, keyPair, peerAddress, ledgersListener, new HashDigest[]{ledger});
    }

    static LedgerPeerConnectionManager newMockLedgerPeerConnectionManager(HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress peerAddress, LedgersListener ledgersListener, HashDigest[] accessibleLedgers) {
        LedgerPeerConnectionManager mConnectionManager = spy(new LedgerPeerConnectionManager(ledger, peerAddress, keyPair, null, clientManager, ledgersListener));
        doReturn(accessibleLedgers).when(mConnectionManager).connect();
        GatewayAuthResponse authResponse = new GatewayAuthResponse();
        LedgerIncomingSettings[] settings = new LedgerIncomingSettings[accessibleLedgers.length];
        for (int i = 0; i < settings.length; i++) {
            settings[i] = new LedgerIncomingSettings();
            settings[i].setLedgerHash(accessibleLedgers[i]);
        }
        authResponse.setLedgers(settings);
        doReturn(authResponse).when(mConnectionManager).auth();
        doReturn(1l).when(mConnectionManager).ping();
        doReturn(false).doReturn(true).when(mConnectionManager).connected();

        return mConnectionManager;
    }

    static LedgerPeerConnectionManager newMockLedgerPeerConnectionManager(LedgerPeerConnectionListener connectionListener, HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress peerAddress) {
        LedgerPeerConnectionManager manager = newMockLedgerPeerConnectionManager(ledger, keyPair, peerAddress, null);
        manager.setConnectionListener(connectionListener);
        return manager;
    }

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress[] peerAddresses, LedgersListener ledgersListener) {
        LedgerPeerConnectionManager[] connectionManagers = new LedgerPeerConnectionManager[peerAddresses.length];
        for (int i = 0; i < peerAddresses.length; i++) {
            connectionManagers[i] = newMockLedgerPeerConnectionManager(ledger, keyPair, peerAddresses[i], ledgersListener);
        }

        return newMockLedgerPeersManager(ledger, keyPair, connectionManagers, ledgersListener);
    }

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress peerAddress, LedgersListener ledgersListener) {
        return newMockLedgerPeersManager(ledger, keyPair, new NetworkAddress[]{peerAddress}, ledgersListener);
    }

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, LedgerPeerConnectionManager[] connectionManagers, LedgersListener ledgersListener) {
        LedgerPeersManager ledgerPeersManager = new LedgerPeersManager(ledger, connectionManagers, keyPair, null, clientManager, ledgersListener, null);
        LedgerPeersManager mLedgerPeersManager = spy(ledgerPeersManager);
        for(LedgerPeerConnectionManager manager : connectionManagers) {
            manager.setConnectionListener(mLedgerPeersManager);
        }
        return mLedgerPeersManager;
    }

    /**
     * 测试初始化：
     * 单账本，四节点
     */
    @Test
    public void testInitLedger() throws InterruptedException {
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest ledger = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        NetworkAddress[] peerAddresses = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        Set<NetworkAddress> topology = new HashSet<>(Arrays.asList(peerAddresses));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        doReturn(new HashDigest[]{ledger}).when(mLedgersManager).getLedgers(any(), any());
        LedgerPeersManager peersManager = newMockLedgerPeersManager(ledger, keyPair, peerAddresses[0], mLedgersManager);
        doReturn(topology).when(peersManager).updateTopology();
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(peersManager, ledger, keyPair, peer)).when(peersManager).newPeerConnectionManager(peer);
        }
        doReturn(peersManager).when(mLedgersManager).newLedgerPeersManager(ledger, keyPair, peerAddresses[0]);
        mLedgersManager.init(peerAddresses[0], keyPair, false);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        try {
            for (int i = 0; i < 10000; i++) {
                mLedgersManager.getQueryService(ledger);
                mLedgersManager.getTransactionService(ledger);
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        Assert.assertEquals(1, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager.getPeerAddresses().size());
        Assert.assertTrue(peersManager.getPeerAddresses().containsAll(topology));

        mLedgersManager.close();
    }

    /**
     * 测试初始化：
     * 双账本，四节点
     */
    @Test
    public void testInitLedgers() throws InterruptedException {
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest l0 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        HashDigest l1 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5hJQ7nWjm3HCD5B422ab8TnfzE3Uo4vLpD4hgrZPs1Dqf"));
        NetworkAddress[] peerAddresses0 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        NetworkAddress[] peerAddresses1 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        Set<NetworkAddress> topology0 = new HashSet<>(Arrays.asList(peerAddresses0));
        Set<NetworkAddress> topology1 = new HashSet<>(Arrays.asList(peerAddresses1));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        // 从0节点初始化
        doReturn(new HashDigest[]{l0, l1}).when(mLedgersManager).getLedgers(keyPair, peerAddresses0[0]);

        // mock for ledger 0
        LedgerPeersManager peersManager0 = newMockLedgerPeersManager(l0, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(topology0).when(peersManager0).updateTopology();
        doReturn(peersManager0).when(mLedgersManager).newLedgerPeersManager(l0, keyPair, peerAddresses0[0]);

        // mock for ledger 1
        LedgerPeersManager peersManager1 = newMockLedgerPeersManager(l1, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        LedgerPeerConnectionManager l1pcm00 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm01 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm00).doReturn(l1pcm01).when(peersManager1).newPeerConnectionManager(peerAddresses1[0]);
        LedgerPeerConnectionManager l1pcm10 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm11 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm10).doReturn(l1pcm11).when(peersManager1).newPeerConnectionManager(peerAddresses1[1]);
        LedgerPeerConnectionManager l1pcm20 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm21 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm20).doReturn(l1pcm21).when(peersManager1).newPeerConnectionManager(peerAddresses1[2]);
        LedgerPeerConnectionManager l1pcm30 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm31 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm30).doReturn(l1pcm31).when(peersManager1).newPeerConnectionManager(peerAddresses1[3]);
        doReturn(topology1).when(peersManager1).updateTopology();
        doReturn(peersManager1).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peerAddresses0[0]);

        mLedgersManager.init(peerAddresses0[0], keyPair, false);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                mLedgersManager.getQueryService(l0);
                mLedgersManager.getTransactionService(l0);
                mLedgersManager.getQueryService(l1);
                mLedgersManager.getTransactionService(l1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Should never come here");
        }

        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));

        mLedgersManager.close();
    }

    /**
     * 测试链状拓扑网络初始化
     * 四账本:
     * L0: 0,1,2,3
     * L1: 3,4,5,6
     * L2: 6,7,8,9
     * 测试从0节点启动，完成整个拓扑的初始化
     */
    @Test
    public void testInitChainTopology() throws InterruptedException {
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest l0 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        HashDigest l1 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kxFLym4PESNwoJvzvw7LX2ZvZtXZpD8uhVL6T8HXaVNB"));
        HashDigest l2 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5oj3s7PVhcoxYUXrdbzAdE7mitKmMRaB3MXkHW2KAZNFM"));
        NetworkAddress[] peerAddresses0 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        NetworkAddress[] peerAddresses1 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7083),
                new NetworkAddress("127.0.0.1", 7084),
                new NetworkAddress("127.0.0.1", 7085),
                new NetworkAddress("127.0.0.1", 7086),
        };
        NetworkAddress[] peerAddresses2 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7086),
                new NetworkAddress("127.0.0.1", 7087),
                new NetworkAddress("127.0.0.1", 7088),
                new NetworkAddress("127.0.0.1", 7089),
        };
        Set<NetworkAddress> topology0 = new HashSet<>(Arrays.asList(peerAddresses0));
        Set<NetworkAddress> topology1 = new HashSet<>(Arrays.asList(peerAddresses1));
        Set<NetworkAddress> topology2 = new HashSet<>(Arrays.asList(peerAddresses2));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        // 从0节点初始化
        doReturn(new HashDigest[]{l0}).when(mLedgersManager).getLedgers(keyPair, peerAddresses0[0]);

        // mock for ledger 0
        LedgerPeersManager peersManager0 = newMockLedgerPeersManager(l0, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0})}, mLedgersManager);
        doReturn(topology0).when(peersManager0).updateTopology();
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0})).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0})).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0})).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(peersManager0).when(mLedgersManager).newLedgerPeersManager(l0, keyPair, peerAddresses0[0]);

        // mock for ledger 1
        LedgerPeersManager peersManager1 = newMockLedgerPeersManager(l1, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l1})}, mLedgersManager);
        doReturn(topology1).when(peersManager1).updateTopology();
        doReturn(newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager1).newPeerConnectionManager(peerAddresses1[0]);
        doReturn(newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l1})).when(peersManager1).newPeerConnectionManager(peerAddresses1[1]);
        doReturn(newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l1})).when(peersManager1).newPeerConnectionManager(peerAddresses1[2]);
        doReturn(newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l1, l2})).when(peersManager1).newPeerConnectionManager(peerAddresses1[3]);
        doReturn(peersManager1).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peerAddresses1[0]);

        // mock for ledger 2
        LedgerPeersManager peersManager2 = newMockLedgerPeersManager(l2, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[0], mLedgersManager, new HashDigest[]{l1, l2})}, mLedgersManager);
        doReturn(topology2).when(peersManager2).updateTopology();
        doReturn(newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[0], mLedgersManager, new HashDigest[]{l1, l2})).when(peersManager2).newPeerConnectionManager(peerAddresses2[0]);
        doReturn(newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[1], mLedgersManager, new HashDigest[]{l2})).when(peersManager2).newPeerConnectionManager(peerAddresses2[1]);
        doReturn(newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[2], mLedgersManager, new HashDigest[]{l2})).when(peersManager2).newPeerConnectionManager(peerAddresses2[2]);
        doReturn(newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[3], mLedgersManager, new HashDigest[]{l2})).when(peersManager2).newPeerConnectionManager(peerAddresses2[3]);
        doReturn(peersManager2).when(mLedgersManager).newLedgerPeersManager(l2, keyPair, peerAddresses2[0]);

        mLedgersManager.init(peerAddresses0[0], keyPair, false);

        Thread.sleep((LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL) * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                mLedgersManager.getQueryService(l0);
                mLedgersManager.getTransactionService(l0);
                mLedgersManager.getQueryService(l1);
                mLedgersManager.getTransactionService(l1);
                mLedgersManager.getQueryService(l2);
                mLedgersManager.getTransactionService(l2);
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        Assert.assertEquals(3, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));
        Assert.assertEquals(4, peersManager2.getPeerAddresses().size());
        Assert.assertTrue(peersManager2.getPeerAddresses().containsAll(topology2));

        mLedgersManager.close();
    }

    /**
     * 测试环状拓扑网络初始化
     * 四账本:
     * L0: 0,1,2,3
     * L1: 3,4,5,6
     * L2: 6,7,8,0
     * 测试从0节点启动，完成整个拓扑的初始化
     */
    @Test
    public void testInitRingTopology() throws InterruptedException {
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest l0 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        HashDigest l1 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kxFLym4PESNwoJvzvw7LX2ZvZtXZpD8uhVL6T8HXaVNB"));
        HashDigest l2 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5oj3s7PVhcoxYUXrdbzAdE7mitKmMRaB3MXkHW2KAZNFM"));
        NetworkAddress[] peerAddresses0 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        NetworkAddress[] peerAddresses1 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7083),
                new NetworkAddress("127.0.0.1", 7084),
                new NetworkAddress("127.0.0.1", 7085),
                new NetworkAddress("127.0.0.1", 7086),
        };
        NetworkAddress[] peerAddresses2 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7086),
                new NetworkAddress("127.0.0.1", 7087),
                new NetworkAddress("127.0.0.1", 7088),
                new NetworkAddress("127.0.0.1", 7080),
        };
        Set<NetworkAddress> topology0 = new HashSet<>(Arrays.asList(peerAddresses0));
        Set<NetworkAddress> topology1 = new HashSet<>(Arrays.asList(peerAddresses1));
        Set<NetworkAddress> topology2 = new HashSet<>(Arrays.asList(peerAddresses2));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        // 从0节点初始化
        doReturn(new HashDigest[]{l0}).when(mLedgersManager).getLedgers(keyPair, peerAddresses0[0]);

        // mock for ledger 0
        LedgerPeersManager peersManager0 = newMockLedgerPeersManager(l0, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l2})}, mLedgersManager);
        doReturn(topology0).when(peersManager0).updateTopology();
        LedgerPeerConnectionManager l0pcp00 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l2});
        LedgerPeerConnectionManager l0pcm01 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l2});
        doReturn(l0pcp00).doReturn(l0pcm01).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        LedgerPeerConnectionManager l0pcm10 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0});
        LedgerPeerConnectionManager l0pcm11 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0});
        doReturn(l0pcm10).doReturn(l0pcm11).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        LedgerPeerConnectionManager l0pcm20 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0});
        LedgerPeerConnectionManager l0pcm21 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0});
        doReturn(l0pcm20).doReturn(l0pcm21).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        LedgerPeerConnectionManager l0pcm30 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l0pcm31 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l0pcm30).doReturn(l0pcm31).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(peersManager0).when(mLedgersManager).newLedgerPeersManager(l0, keyPair, peerAddresses0[0]);

        // mock for ledger 1
        LedgerPeersManager peersManager1 = newMockLedgerPeersManager(l1, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        doReturn(topology1).when(peersManager1).updateTopology();
        LedgerPeerConnectionManager l1pcm00 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm01 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm00).doReturn(l1pcm01).when(peersManager1).newPeerConnectionManager(peerAddresses1[0]);
        LedgerPeerConnectionManager l1pcm10 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l1});
        LedgerPeerConnectionManager l1pcm11 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l1});
        doReturn(l1pcm10).doReturn(l1pcm11).when(peersManager1).newPeerConnectionManager(peerAddresses1[1]);
        LedgerPeerConnectionManager l1pcm20 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l1});
        LedgerPeerConnectionManager l1pcm21 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l1});
        doReturn(l1pcm20).doReturn(l1pcm21).when(peersManager1).newPeerConnectionManager(peerAddresses1[2]);
        LedgerPeerConnectionManager l1pcm30 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l1, l2});
        LedgerPeerConnectionManager l1pcm31 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l1, l2});
        doReturn(l1pcm30).doReturn(l1pcm31).when(peersManager1).newPeerConnectionManager(peerAddresses1[3]);
        doReturn(peersManager1).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peerAddresses1[0]);
        doReturn(peersManager1).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peerAddresses1[3]);

        // mock for ledger 2
        LedgerPeersManager peersManager2 = newMockLedgerPeersManager(l2, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[0], mLedgersManager, new HashDigest[]{l1, l2})}, mLedgersManager);
        doReturn(topology2).when(peersManager2).updateTopology();
        LedgerPeerConnectionManager l2pcm00 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[0], mLedgersManager, new HashDigest[]{l1, l2});
        LedgerPeerConnectionManager l2pcm01 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[0], mLedgersManager, new HashDigest[]{l1, l2});
        doReturn(l2pcm00).doReturn(l2pcm01).when(peersManager2).newPeerConnectionManager(peerAddresses2[0]);
        LedgerPeerConnectionManager l2pcm10 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[1], mLedgersManager, new HashDigest[]{l2});
        LedgerPeerConnectionManager l2pcm11 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[1], mLedgersManager, new HashDigest[]{l2});
        doReturn(l2pcm10).doReturn(l2pcm11).when(peersManager2).newPeerConnectionManager(peerAddresses2[1]);
        LedgerPeerConnectionManager l2pcm20 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[2], mLedgersManager, new HashDigest[]{l2});
        LedgerPeerConnectionManager l2pcm21 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[2], mLedgersManager, new HashDigest[]{l2});
        doReturn(l2pcm20).doReturn(l2pcm21).when(peersManager2).newPeerConnectionManager(peerAddresses2[2]);
        LedgerPeerConnectionManager l2pcm30 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[3], mLedgersManager, new HashDigest[]{l0, l2});
        LedgerPeerConnectionManager l2pcm31 = newMockLedgerPeerConnectionManager(l2, keyPair, peerAddresses2[3], mLedgersManager, new HashDigest[]{l0, l2});
        doReturn(l2pcm30).doReturn(l2pcm31).when(peersManager2).newPeerConnectionManager(peerAddresses2[3]);
        doReturn(peersManager2).when(mLedgersManager).newLedgerPeersManager(l2, keyPair, peerAddresses2[0]);
        doReturn(peersManager2).when(mLedgersManager).newLedgerPeersManager(l2, keyPair, peerAddresses2[3]);

        mLedgersManager.init(peerAddresses0[0], keyPair, false);

        Thread.sleep((LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL) * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                mLedgersManager.getQueryService(l0);
                mLedgersManager.getTransactionService(l0);
                mLedgersManager.getQueryService(l1);
                mLedgersManager.getTransactionService(l1);
                mLedgersManager.getQueryService(l2);
                mLedgersManager.getTransactionService(l2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Should never come here");
        }

        Assert.assertEquals(3, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));
        Assert.assertEquals(4, peersManager2.getPeerAddresses().size());
        Assert.assertTrue(peersManager2.getPeerAddresses().containsAll(topology2));

        mLedgersManager.close();
    }

    /**
     * 测试单账本中节点变化
     */
    @Test
    public void testPeerChangeInLedger() throws InterruptedException {
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest ledger = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        NetworkAddress[] peers = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        Set<NetworkAddress> topology = new HashSet<>(Arrays.asList(peers));

        LedgersManager mLedgersManager = spy(new LedgersManager());

        // 初始化账本(peer0/1/2/3)
        doReturn(new HashDigest[]{ledger}).when(mLedgersManager).getLedgers(keyPair, peers[0]);
        LedgerPeersManager peersManager = newMockLedgerPeersManager(ledger, keyPair, peers[0], mLedgersManager);
        doReturn(topology).when(peersManager).updateTopology();
        doReturn(peersManager).when(mLedgersManager).newLedgerPeersManager(ledger, keyPair, peers[0]);
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, keyPair, peer, mLedgersManager)).when(peersManager).newPeerConnectionManager(peer);
        }
        mLedgersManager.init(peers[0], keyPair, false);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertEquals(4, peersManager.getPeerAddresses().size());
        Assert.assertTrue(topology.containsAll(peersManager.getPeerAddresses()));

        // 添加节点peer4
        NetworkAddress peer4 = new NetworkAddress("127.0.0.1", 7084);
        topology.add(peer4);
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, keyPair, peer, mLedgersManager)).when(peersManager).newPeerConnectionManager(peer);
        }
        doReturn(topology).when(peersManager).updateTopology();
        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertEquals(5, peersManager.getPeerAddresses().size());
        Assert.assertTrue(topology.containsAll(peersManager.getPeerAddresses()));

        // 移除peer4
        topology.remove(new NetworkAddress("127.0.0.1", 7084));
        for (NetworkAddress peer : topology) {
            doReturn(newMockLedgerPeerConnectionManager(ledger, keyPair, peer, mLedgersManager)).when(peersManager).newPeerConnectionManager(peer);
        }
        doReturn(topology).when(peersManager).updateTopology();
        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertEquals(1, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager.getPeerAddresses().size());
        Assert.assertTrue(topology.containsAll(peersManager.getPeerAddresses()));

        mLedgersManager.close();
    }

    /**
     * 测试多账本中节点变化
     */
    @Test
    public void testPeerChangeInLedgers() throws InterruptedException {
        // 初始化双账本
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest l0 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        HashDigest l1 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5hJQ7nWjm3HCD5B422ab8TnfzE3Uo4vLpD4hgrZPs1Dqf"));
        NetworkAddress[] peerAddresses0 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        NetworkAddress[] peerAddresses1 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        Set<NetworkAddress> topology0 = new HashSet<>(Arrays.asList(peerAddresses0));
        Set<NetworkAddress> topology1 = new HashSet<>(Arrays.asList(peerAddresses1));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        // 从0节点初始化
        doReturn(new HashDigest[]{l0}).when(mLedgersManager).getLedgers(keyPair, peerAddresses0[0]);

        // mock for ledger 0
        LedgerPeersManager peersManager0 = newMockLedgerPeersManager(l0, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(topology0).when(peersManager0).updateTopology();
        doReturn(peersManager0).when(mLedgersManager).newLedgerPeersManager(l0, keyPair, peerAddresses0[0]);

        // mock for ledger 1
        LedgerPeersManager peersManager1 = newMockLedgerPeersManager(l1, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        LedgerPeerConnectionManager l1pcm00 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm01 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm00).doReturn(l1pcm01).when(peersManager1).newPeerConnectionManager(peerAddresses1[0]);
        LedgerPeerConnectionManager l1pcm10 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm11 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm10).doReturn(l1pcm11).when(peersManager1).newPeerConnectionManager(peerAddresses1[1]);
        LedgerPeerConnectionManager l1pcm20 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm21 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm20).doReturn(l1pcm21).when(peersManager1).newPeerConnectionManager(peerAddresses1[2]);
        LedgerPeerConnectionManager l1pcm30 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm31 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1});
        doReturn(l1pcm30).doReturn(l1pcm31).when(peersManager1).newPeerConnectionManager(peerAddresses1[3]);
        doReturn(topology1).when(peersManager1).updateTopology();
        doReturn(peersManager1).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peerAddresses0[0]);

        mLedgersManager.init(peerAddresses0[0], keyPair, false);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL * 2);
        try {
            for (int i = 0; i < 10000; i++) {
                mLedgersManager.getQueryService(l0);
                mLedgersManager.getTransactionService(l0);
                mLedgersManager.getQueryService(l1);
                mLedgersManager.getTransactionService(l1);
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));

        // ledger 0 添加节点peer4
        NetworkAddress peer4 = new NetworkAddress("127.0.0.1", 7084);
        topology0.add(peer4);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peer4, mLedgersManager, new HashDigest[]{l0})).when(peersManager0).newPeerConnectionManager(peer4);
        doReturn(topology0).when(peersManager0).updateTopology();

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(5, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));

        // ledger 0 移除peer4
        topology0.remove(new NetworkAddress("127.0.0.1", 7084));
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        doReturn(newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1})).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(topology0).when(peersManager0).updateTopology();

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL);
        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));

        mLedgersManager.close();
    }

    /**
     * 测试账本添加
     */
    @Test
    public void testLedgerAdd() throws InterruptedException {
        // 初始化单账本
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest l0 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        NetworkAddress[] peerAddresses = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        Set<NetworkAddress> topology = new HashSet<>(Arrays.asList(peerAddresses));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        doReturn(new HashDigest[]{l0}).when(mLedgersManager).getLedgers(any(), any());
        LedgerPeersManager l0PeersManager = newMockLedgerPeersManager(l0, keyPair, peerAddresses[0], mLedgersManager);
        LedgerPeerConnectionManager[] l0PeerConnectionManagers = new LedgerPeerConnectionManager[peerAddresses.length];
        for (int i = 0; i < peerAddresses.length; i++) {
            LedgerPeerConnectionManager lpcm = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses[i], mLedgersManager);
            l0PeerConnectionManagers[i] = lpcm;
            doReturn(lpcm).when(l0PeersManager).newPeerConnectionManager(peerAddresses[i]);
        }
        doReturn(topology).when(l0PeersManager).updateTopology();
        doReturn(l0PeersManager).when(mLedgersManager).newLedgerPeersManager(l0, keyPair, peerAddresses[0]);
        mLedgersManager.init(peerAddresses[0], keyPair, false);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL);
        try {
            for (int i = 0; i < 10000; i++) {
                mLedgersManager.getQueryService(l0);
                mLedgersManager.getTransactionService(l0);
            }
        } catch (Exception e) {
            Assert.fail("Should never come here");
        }

        Assert.assertEquals(1, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, l0PeersManager.getPeerAddresses().size());
        Assert.assertTrue(l0PeersManager.getPeerAddresses().containsAll(topology));

        // 新增账本
        HashDigest l1 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5hJQ7nWjm3HCD5B422ab8TnfzE3Uo4vLpD4hgrZPs1Dqf"));
        LedgerPeersManager l1PeersManager = newMockLedgerPeersManager(l1, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        doReturn(topology).when(l1PeersManager).updateTopology();
        for (int i = 0; i < peerAddresses.length; i++) {
            LedgerPeerConnectionManager lpcm0 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses[i], mLedgersManager, new HashDigest[]{l0, l1});
            LedgerPeerConnectionManager lpcm1 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses[i], mLedgersManager, new HashDigest[]{l0, l1});
            doReturn(lpcm0).doReturn(lpcm1).when(l1PeersManager).newPeerConnectionManager(peerAddresses[i]);
        }
        for (NetworkAddress peer : topology) {
            doReturn(l1PeersManager).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peer);
        }
        LedgerIncomingSettings[] addSettings = new LedgerIncomingSettings[]{new LedgerIncomingSettings(), new LedgerIncomingSettings()};
        addSettings[0].setLedgerHash(l0);
        addSettings[1].setLedgerHash(l1);
        GatewayAuthResponse addAuthResponse = new GatewayAuthResponse();
        addAuthResponse.setLedgers(addSettings);
        for (LedgerPeerConnectionManager peerConnectionManager : l0PeerConnectionManagers) {
            doReturn(new HashDigest[]{l0, l1}).when(peerConnectionManager).connect();
            doReturn(addAuthResponse).when(peerConnectionManager).auth();
        }

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL);
        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, l0PeersManager.getPeerAddresses().size());
        Assert.assertTrue(l0PeersManager.getPeerAddresses().containsAll(topology));
        Assert.assertEquals(4, l1PeersManager.getPeerAddresses().size());
        Assert.assertTrue(l1PeersManager.getPeerAddresses().containsAll(topology));

        mLedgersManager.close();
    }

    /**
     * 测试账本移除
     */
    @Test
    public void testLedgerRemove() throws InterruptedException {
        AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
                KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                        "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
        HashDigest l0 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
        HashDigest l1 = Crypto.resolveAsHashDigest(Base58Utils.decode("j5hJQ7nWjm3HCD5B422ab8TnfzE3Uo4vLpD4hgrZPs1Dqf"));
        NetworkAddress[] peerAddresses0 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        NetworkAddress[] peerAddresses1 = new NetworkAddress[]{
                new NetworkAddress("127.0.0.1", 7080),
                new NetworkAddress("127.0.0.1", 7081),
                new NetworkAddress("127.0.0.1", 7082),
                new NetworkAddress("127.0.0.1", 7083),
        };
        Set<NetworkAddress> topology0 = new HashSet<>(Arrays.asList(peerAddresses0));
        Set<NetworkAddress> topology1 = new HashSet<>(Arrays.asList(peerAddresses1));

        LedgersManager mLedgersManager = spy(new LedgersManager());
        // 从0节点初始化
        doReturn(new HashDigest[]{l0}).when(mLedgersManager).getLedgers(keyPair, peerAddresses0[0]);

        Set<LedgerPeerConnectionManager> peerConnectionManagers = new HashSet<>();

        // mock for ledger 0
        LedgerPeersManager peersManager0 = newMockLedgerPeersManager(l0, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        LedgerPeerConnectionManager l0pcm0 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l0pcm0);
        doReturn(l0pcm0).when(peersManager0).newPeerConnectionManager(peerAddresses0[0]);
        LedgerPeerConnectionManager l0pcm1 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l0pcm1);
        doReturn(l0pcm1).when(peersManager0).newPeerConnectionManager(peerAddresses0[1]);
        LedgerPeerConnectionManager l0pcm2 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l0pcm2);
        doReturn(l0pcm2).when(peersManager0).newPeerConnectionManager(peerAddresses0[2]);
        LedgerPeerConnectionManager l0pcm3 = newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l0pcm3);
        doReturn(l0pcm3).when(peersManager0).newPeerConnectionManager(peerAddresses0[3]);
        doReturn(topology0).when(peersManager0).updateTopology();
        doReturn(peersManager0).when(mLedgersManager).newLedgerPeersManager(l0, keyPair, peerAddresses0[0]);

        // mock for ledger 1
        LedgerPeersManager peersManager1 = newMockLedgerPeersManager(l1, keyPair,
                new LedgerPeerConnectionManager[]{newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1})}, mLedgersManager);
        LedgerPeerConnectionManager l1pcm00 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm01 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l1pcm00);
        doReturn(l1pcm00).doReturn(l1pcm01).when(peersManager1).newPeerConnectionManager(peerAddresses1[0]);
        LedgerPeerConnectionManager l1pcm10 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm11 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l1pcm10);
        peerConnectionManagers.add(l1pcm11);
        doReturn(l1pcm10).doReturn(l1pcm11).when(peersManager1).newPeerConnectionManager(peerAddresses1[1]);
        LedgerPeerConnectionManager l1pcm20 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm21 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l1pcm20);
        peerConnectionManagers.add(l1pcm21);
        doReturn(l1pcm20).doReturn(l1pcm21).when(peersManager1).newPeerConnectionManager(peerAddresses1[2]);
        LedgerPeerConnectionManager l1pcm30 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1});
        LedgerPeerConnectionManager l1pcm31 = newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1});
        peerConnectionManagers.add(l1pcm30);
        peerConnectionManagers.add(l1pcm31);
        doReturn(l1pcm30).doReturn(l1pcm31).when(peersManager1).newPeerConnectionManager(peerAddresses1[3]);
        doReturn(topology1).when(peersManager1).updateTopology();
        doReturn(peersManager1).when(mLedgersManager).newLedgerPeersManager(l1, keyPair, peerAddresses1[0]);

        mLedgersManager.init(peerAddresses0[0], keyPair, false);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL * 2);
        Assert.assertEquals(2, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));
        Assert.assertEquals(4, peersManager1.getPeerAddresses().size());
        Assert.assertTrue(peersManager1.getPeerAddresses().containsAll(topology1));

        // 移除账本
        topology1.clear();
        doReturn(topology1).when(peersManager1).updateTopology();
        LedgerIncomingSettings[] removeSettings = new LedgerIncomingSettings[]{new LedgerIncomingSettings()};
        removeSettings[0].setLedgerHash(l0);
        GatewayAuthResponse removeAuthResponse = new GatewayAuthResponse();
        removeAuthResponse.setLedgers(removeSettings);
        for (LedgerPeerConnectionManager peerConnectionManager : peerConnectionManagers) {
            doReturn(new HashDigest[]{l0}).when(peerConnectionManager).connect();
            doReturn(removeAuthResponse).when(peerConnectionManager).auth();
        }

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL*3);
        Assert.assertEquals(1, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));

        mLedgersManager.close();
    }
}
