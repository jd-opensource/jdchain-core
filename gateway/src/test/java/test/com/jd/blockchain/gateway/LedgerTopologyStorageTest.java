package test.com.jd.blockchain.gateway;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.gateway.service.GatewayConsensusClientManager;
import com.jd.blockchain.gateway.service.LedgerPeerConnectionManager;
import com.jd.blockchain.gateway.service.LedgerPeersManager;
import com.jd.blockchain.gateway.service.LedgersListener;
import com.jd.blockchain.gateway.service.LedgersManager;
import com.jd.blockchain.gateway.service.topology.LedgerPeerApiServicesTopology;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopologyStorage;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.setting.GatewayAuthResponse;
import com.jd.blockchain.setting.LedgerIncomingSettings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import utils.codec.Base58Utils;
import utils.io.FileSystemStorage;
import utils.net.NetworkAddress;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * 账本-节点拓扑存储测试
 */
@Ignore("耗时太长，不同机器环境不一定都能通过，测试用例需要重新设计")
public class LedgerTopologyStorageTest {

    static ConsensusClientManager clientManager = new GatewayConsensusClientManager();

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
    LedgerPeersTopologyStorage topologyStorage;

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

    static LedgerPeersManager newMockLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, LedgerPeerConnectionManager[] connectionManagers, LedgersListener ledgersListener, LedgerPeersTopologyStorage storage) {
        LedgerPeersManager ledgerPeersManager = new LedgerPeersManager(ledger, connectionManagers, keyPair, null, clientManager, ledgersListener, storage);
        LedgerPeersManager mLedgerPeersManager = spy(ledgerPeersManager);
        for(LedgerPeerConnectionManager manager : connectionManagers) {
            manager.setConnectionListener(mLedgerPeersManager);
        }

        return mLedgerPeersManager;
    }

    @Before
    public void before() throws IOException {
        topologyStorage = new LedgerPeersTopologyStorage(new FileSystemStorage(LedgerTopologyStorageTest.class.getResource("/").getPath()));
        topologyStorage.setTopology(l0.toBase58(), new LedgerPeerApiServicesTopology(l0, keyPair, topology0));
        topologyStorage.setTopology(l1.toBase58(), new LedgerPeerApiServicesTopology(l1, keyPair, topology1));
    }

    /**
     * 测试从存储初始化双账本，然后移除一个账本
     */
    @Test
    public void testInitFromStorage() throws InterruptedException {
        // 初始化双账本环境
        LedgersManager mLedgersManager = spy(new LedgersManager());
        // mock for ledger 0
        LedgerPeerConnectionManager[] peerConnectionManagers0 = new LedgerPeerConnectionManager[]{
                newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[0], mLedgersManager, new HashDigest[]{l0, l1}),
                newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[1], mLedgersManager, new HashDigest[]{l0, l1}),
                newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[2], mLedgersManager, new HashDigest[]{l0, l1}),
                newMockLedgerPeerConnectionManager(l0, keyPair, peerAddresses0[3], mLedgersManager, new HashDigest[]{l0, l1}),
        };
        LedgerPeersManager peersManager0 = newMockLedgerPeersManager(l0, keyPair, peerConnectionManagers0, mLedgersManager, topologyStorage);
        doReturn(topology0).when(peersManager0).updateTopology();

        // mock for ledger 1
        LedgerPeerConnectionManager[] peerConnectionManagers1 = new LedgerPeerConnectionManager[]{
                newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[0], mLedgersManager, new HashDigest[]{l0, l1}),
                newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[1], mLedgersManager, new HashDigest[]{l0, l1}),
                newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[2], mLedgersManager, new HashDigest[]{l0, l1}),
                newMockLedgerPeerConnectionManager(l1, keyPair, peerAddresses1[3], mLedgersManager, new HashDigest[]{l0, l1}),
        };
        LedgerPeersManager peersManager1 = newMockLedgerPeersManager(l1, keyPair, peerConnectionManagers1, mLedgersManager, topologyStorage);
        doReturn(topology1).when(peersManager1).updateTopology();

        // 先初始化L1
        doReturn(peersManager1).doReturn(peersManager0).when(mLedgersManager).newLedgerPeersManager(Mockito.any(), Mockito.any(), Mockito.any(NetworkAddress[].class));

        doReturn(topologyStorage).when(mLedgersManager).newLedgerPeersTopologyStorage();
        mLedgersManager.init(peerAddresses0[0], keyPair, true);

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL);
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
        for (LedgerPeerConnectionManager peerConnectionManager : peerConnectionManagers0) {
            doReturn(new HashDigest[]{l0}).when(peerConnectionManager).connect();
            doReturn(removeAuthResponse).when(peerConnectionManager).auth();
        }
        for (LedgerPeerConnectionManager peerConnectionManager : peerConnectionManagers1) {
            doReturn(new HashDigest[]{l0}).when(peerConnectionManager).connect();
            doReturn(removeAuthResponse).when(peerConnectionManager).auth();
        }

        Thread.sleep(LedgerPeersManager.UPDATE_TOPOLOGY_INTERVAL + LedgerPeerConnectionManager.AUTH_INTERVAL);
        Assert.assertEquals(1, mLedgersManager.getLedgerHashs().length);
        Assert.assertEquals(4, peersManager0.getPeerAddresses().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(topology0));

        // 验证存储
        Assert.assertEquals(2, topologyStorage.getLedgers().size());
        Assert.assertTrue(peersManager0.getPeerAddresses().containsAll(new HashSet<>(Arrays.asList(topologyStorage.getTopology(l0.toBase58()).getPeerAddresses()))));
        Assert.assertEquals(0, topologyStorage.getTopology(l1.toBase58()).getPeerAddresses().length);
    }

}
