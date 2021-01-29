package test.com.jd.blockchain.gateway;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.gateway.service.LedgerPeerConnectionManager;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.setting.GatewayAuthResponse;
import com.jd.blockchain.setting.LedgerIncomingSettings;
import org.junit.Assert;
import org.junit.Test;
import utils.codec.Base58Utils;
import utils.net.NetworkAddress;

import static com.jd.blockchain.gateway.service.LedgerPeerConnectionManager.AUTH_INTERVAL;
import static com.jd.blockchain.gateway.service.LedgerPeerConnectionManager.PING_INTERVAL;
import static org.mockito.Mockito.*;

/**
 * 节点连接测试
 */
public class LedgerPeerConnectionManagerTest {

    static AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey("3snPdw7i7PhgdrXp9UxgTMr5PAYFxrEWdRdAdn9hsBA4pvp1iVYXM6"),
            KeyGenUtils.decodePrivKey("177gjvG9ZKkGwdzKfrK2YguhS2Wthu6EdbVSF9WqCxfmqdJuVz82BfFwt53XaGYEbp8RqRW",
                    "8EjkXVSTxMFjCvNNsTo8RBMDEVQmk7gYkW4SCDuvdsBG"));
    static HashDigest ledger = Crypto.resolveAsHashDigest(Base58Utils.decode("j5kVBDweKVYVXBmUS3TRE2r9UrqPH1ojt8PKuP6rfF2UYx"));
    static NetworkAddress peerAddress = new NetworkAddress("127.0.0.1", 7080);

    static LedgerPeerConnectionManager newMockLedgerPeerConnectionManager(HashDigest ledger, NetworkAddress peerAddress) {
        LedgerPeerConnectionManager mConnectionManager = spy(new LedgerPeerConnectionManager(ledger, peerAddress, keyPair, null, null, null));
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

    /**
     * 状态测试
     */
    @Test
    public void testState() throws InterruptedException {
        LedgerPeerConnectionManager mConnectionManager = newMockLedgerPeerConnectionManager(ledger, peerAddress);
        mConnectionManager.startTimerTask();

        Thread.sleep(PING_INTERVAL);
        Assert.assertTrue(mConnectionManager.isAvailable());
        Assert.assertTrue(mConnectionManager.isAuthorized());

        GatewayAuthResponse authResponse = new GatewayAuthResponse();
        authResponse.setLedgers(new LedgerIncomingSettings[]{});
        doReturn(authResponse).when(mConnectionManager).auth();
        doReturn(-1l).when(mConnectionManager).ping();

        Thread.sleep(AUTH_INTERVAL);
        Assert.assertFalse(mConnectionManager.isAvailable());
        Assert.assertFalse(mConnectionManager.isAuthorized());
    }

}
