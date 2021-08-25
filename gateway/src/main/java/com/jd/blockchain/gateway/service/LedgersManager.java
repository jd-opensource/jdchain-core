package com.jd.blockchain.gateway.service;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.gateway.event.EventListener;
import com.jd.blockchain.gateway.event.EventListenerService;
import com.jd.blockchain.gateway.event.PullEventListener;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopology;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopologyStorage;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.sdk.service.PeerBlockchainServiceFactory;
import com.jd.blockchain.sdk.service.SessionCredentialProvider;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utils.codec.Base58Utils;
import utils.io.Storage;
import utils.net.NetworkAddress;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 账本-共识节点连接管理
 */
@Component
public class LedgersManager implements LedgersService, LedgersListener, EventListenerService {
    private static final Logger logger = LoggerFactory.getLogger(LedgersManager.class);
    @Autowired
    private SessionCredentialProvider credentialProvider;
    @Autowired
    private ConsensusClientManager clientManager;
    @Autowired
    private Storage runtimeStorage;

    private LedgerPeersTopologyStorage topologyStorage;
    private EventListener eventListener;

    // 账本服务列表
    private Map<HashDigest, LedgerPeersManager> ledgerServices = new ConcurrentHashMap<>();

    private ReadWriteLock ledgersLock = new ReentrantReadWriteLock();

    public LedgersManager() {
    }

    public void init(NetworkAddress address, AsymmetricKeypair keyPair, boolean storageOpen) {
        init(address, keyPair, null, storageOpen);
    }

    public void init(NetworkAddress address, AsymmetricKeypair keyPair, X509Certificate certificate, boolean storageOpen) {
        if (storageOpen) {
            this.topologyStorage = newLedgerPeersTopologyStorage();
        }

        ledgersLock.writeLock().lock();
        try {
            // 是否开启拓扑存储
            if (!storageOpen) {
                // 未开启拓扑存储，使用配置文件初始化
                init(address, keyPair, certificate, -1);
            } else {
                Set<String> ledgers = topologyStorage.getLedgers();
                if (ledgers.size() == 0) {
                    // 存储的拓扑为空，使用配置文件初始化
                    init(address, keyPair, certificate, -1);
                } else {
                    initWithStorage(address, keyPair, certificate, ledgers);
                }
            }
        } finally {
            ledgersLock.writeLock().unlock();
        }
    }

    public LedgerPeersTopologyStorage newLedgerPeersTopologyStorage() {
        return new LedgerPeersTopologyStorage(runtimeStorage);
    }

    /**
     * 使用配置文件以及磁盘中保存的拓扑信息初始化
     *
     * @param address          配置文件中节点地址
     * @param keyPair          配置文件中身份信息
     * @param certificate      配置文件中网关证书
     * @param ledgersInStorage 存储中记录的账本信息
     * @return
     */
    private void initWithStorage(NetworkAddress address, AsymmetricKeypair keyPair, X509Certificate certificate, Set<String> ledgersInStorage) {
        // 存储拓扑中是否包含配置文件中节点地址
        boolean configPeerInStorage = false;
        // 存储拓扑中身份信息只包含配置文件中的身份信息
        boolean configKeyPairSameInStorage = true;
        for (String ledger : ledgersInStorage) {
            LedgerPeersTopology topology = topologyStorage.getTopology(ledger);
            if (null == topology.getPeerAddresses()) {
                continue;
            }
            for (NetworkAddress peer : topology.getPeerAddresses()) {
                if (peer.equals(address)) {
                    configPeerInStorage = true;
                }
            }
            if (!topology.getPrivKey().equal(keyPair.getPrivKey().toBytes())) {
                configKeyPairSameInStorage = false;
            }
        }

        // 如果存储拓扑中是否包含配置文件中节点地址，且存储拓扑中的身份信息只包含配置文件中的身份信息，使用存储拓扑初始化
        if (configPeerInStorage && configKeyPairSameInStorage) {
            initFromStorage(ledgersInStorage, keyPair, certificate);
        } else {
            // 根据配置文件初始化，返回可访问账本列表
            HashDigest[] ledgers = init(address, keyPair, certificate, 2);
            if (null == ledgers || ledgers.length == 0) {
                ledgers = new HashDigest[0];
                new Thread(() -> init(address, keyPair, certificate, -1)).start();
            }
            // 根据存储拓扑初始化未初始化账本
            for (int i = 0; null != ledgers && i < ledgers.length; i++) {
                ledgersInStorage.remove(ledgers[i].toBase58());
            }
            initFromStorage(ledgersInStorage, keyPair, certificate);
        }

    }

    /**
     * 根据初始配置地址初始化
     *
     * @param address     配置文件中节点地址
     * @param keyPair     配置文件中身份信息
     * @param certificate 配置文件中网关证书
     * @param retryTimes  重试次数，负数表示无限重试
     * @return 返回初始化的账本列表
     */
    private HashDigest[] init(NetworkAddress address, AsymmetricKeypair keyPair, X509Certificate certificate, int retryTimes) {
        logger.info("Init from config {}, keyPair {}", address, KeyGenUtils.encodePubKey(keyPair.getPubKey()));
        // 从初始连接查询可访问账本列表
        HashDigest[] ledgers = null;
        // 网关初始化时，初始连接失败或者无可访问账本，将持续定时轮询等待
        int count = 0;
        while (count <= retryTimes || retryTimes < 0) {
            ledgers = getLedgers(keyPair, certificate, address);
            if (null != ledgers && ledgers.length > 0) {
                break;
            }
            if (retryTimes >= 0) {
                count++;
            }
            logger.error("No ledger available from {}", address);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                logger.error("InterruptedException", e);
            }
        }

        // 创建账本管理
        ledgersLock.writeLock().lock();
        try {
            for (int i = 0; null != ledgers && i < ledgers.length; i++) {
                if (!ledgerServices.containsKey(ledgers[i]) || !ledgerServices.get(ledgers[i]).isReady()) {
                    if (ledgerServices.containsKey(ledgers[i])) {
                        ledgerServices.get(ledgers[i]).close();
                    }
                    LedgerPeersManager peersManager = newLedgerPeersManager(ledgers[i], keyPair, certificate, address);
                    ledgerServices.put(ledgers[i], peersManager);
                    peersManager.startTimerTask();
                }
            }
        } finally {
            ledgersLock.writeLock().unlock();
        }

        return ledgers;
    }

    /**
     * 从磁盘中保存的拓扑信息初始化
     *
     * @param ledgers 账本列表
     * @param keyPair 接入身份
     * @param certificate 接入身份证书
     * @return
     */
    private boolean initFromStorage(Set<String> ledgers, AsymmetricKeypair keyPair, X509Certificate certificate) {
        logger.info("Init from storage {}, keyPair {}", ledgers, KeyGenUtils.encodePubKey(keyPair.getPubKey()));
        int ledgerSize = 0;
        ledgersLock.writeLock().lock();
        try {
            for (String ledgerHash : ledgers) {
                LedgerPeersTopology topology = topologyStorage.getTopology(ledgerHash);
                if (null != topology) {
                    if (null == topology.getPubKey() || null == topology.getPrivKey()) {
                        continue;
                    }
                    if (null == topology.getPeerAddresses() || topology.getPeerAddresses().length == 0) {
                        continue;
                    }
                    HashDigest ledger = Crypto.resolveAsHashDigest(Base58Utils.decode(ledgerHash));
                    if (!ledgerServices.containsKey(ledger) || !ledgerServices.get(ledger).isReady()) {
                        if (ledgerServices.containsKey(ledger)) {
                            ledgerServices.get(ledger).close();
                        }
                        LedgerPeersManager peersManager = newLedgerPeersManager(ledger, keyPair, certificate, topology.getPeerAddresses());
                        ledgerServices.put(ledger, peersManager);
                        peersManager.startTimerTask();
                    }

                    ledgerSize++;
                }
            }
        } finally {
            ledgersLock.writeLock().unlock();
        }

        return ledgerSize > 0;
    }

    public HashDigest[] getLedgers(AsymmetricKeypair keyPair, NetworkAddress peerAddress) {
        return getLedgers(keyPair, null, peerAddress);
    }

    public HashDigest[] getLedgers(AsymmetricKeypair keyPair, X509Certificate certificate, NetworkAddress peerAddress) {
        try (PeerBlockchainServiceFactory factory = PeerBlockchainServiceFactory.connect(keyPair, certificate, peerAddress, credentialProvider, clientManager);) {
            return factory.getLedgerHashs();
        } catch (Exception e) {
            logger.error("Get ledgers from {} error", peerAddress, e);
        }

        return null;
    }

    @Override
    public EventListener getEventListener() {
        if (eventListener == null) {
            eventListener = new PullEventListener(this);
            eventListener.start();
        }
        return eventListener;
    }

    @Override
    public HashDigest[] getLedgerHashs() {
        ledgersLock.readLock().lock();
        try {
            Set<HashDigest> hashDigests = new HashSet<>();
            for (LedgerPeersManager peersService : ledgerServices.values()) {
                if (peersService.isReady()) {
                    hashDigests.add(peersService.getLedger());
                }
            }
            return hashDigests.toArray(new HashDigest[hashDigests.size()]);
        } finally {
            ledgersLock.readLock().unlock();
        }
    }

    @Override
    public BlockchainQueryService getQueryService(HashDigest ledgerHash) {
        ledgersLock.readLock().lock();
        try {
            LedgerPeersManager service = ledgerServices.get(ledgerHash);
            if (null != service) {
                return service.getQueryService();
            }

            throw new IllegalStateException("Ledger : " + ledgerHash + " not exists!");
        } finally {
            ledgersLock.readLock().unlock();
        }
    }

    @Override
    public TransactionService getTransactionService(HashDigest ledgerHash) {
        ledgersLock.readLock().lock();
        try {
            LedgerPeersManager service = ledgerServices.get(ledgerHash);
            if (null != service) {
                return service.getTransactionService();
            }

            throw new IllegalStateException("Ledger : " + ledgerHash + " not exists!");
        } finally {
            ledgersLock.readLock().unlock();
        }
    }

    /**
     * 重置账本-连接信息
     *
     * @param ledger
     */
    public void reset(HashDigest ledger) {
        ledgersLock.writeLock().lock();
        try {
            if (ledgerServices.containsKey(ledger)) {
                ledgerServices.get(ledger).reset();
            }
        } finally {
            ledgersLock.writeLock().unlock();
        }
    }

    @Override
    public void LedgersUpdated(Set<HashDigest> ledgers, AsymmetricKeypair keyPair, X509Certificate certificate, NetworkAddress peer) {
        ledgersLock.writeLock().lock();
        try {
            for (HashDigest ledger : ledgers) {
                if (ledgerServices.containsKey(ledger)) {
                    ledgerServices.get(ledger).addPeer(peer);
                } else {
                    if (!ledgerServices.containsKey(ledger)) {
                        LedgerPeersManager peersManager = newLedgerPeersManager(ledger, keyPair, certificate, peer);
                        peersManager.startTimerTask();
                        ledgerServices.put(ledger, peersManager);
                    }
                }
            }
        } finally {
            ledgersLock.writeLock().unlock();
        }
    }

    @Override
    public void LedgerRemoved(HashDigest ledger) {
        ledgersLock.writeLock().lock();
        try {
            ledgerServices.remove(ledger);
        } finally {
            ledgersLock.writeLock().unlock();
        }
    }

    public LedgerPeersManager newLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress peerAddress) {
        return newLedgerPeersManager(ledger, keyPair, new NetworkAddress[]{peerAddress});
    }

    public LedgerPeersManager newLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, X509Certificate certificate, NetworkAddress peerAddress) {
        return newLedgerPeersManager(ledger, keyPair, certificate, new NetworkAddress[]{peerAddress});
    }

    public LedgerPeersManager newLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, NetworkAddress[] peerAddresses) {
        return newLedgerPeersManager(ledger, keyPair, null, peerAddresses);
    }

    public LedgerPeersManager newLedgerPeersManager(HashDigest ledger, AsymmetricKeypair keyPair, X509Certificate certificate, NetworkAddress[] peerAddresses) {
        LedgerPeerConnectionManager[] peerConnectionServices = new LedgerPeerConnectionManager[peerAddresses.length];
        for (int i = 0; i < peerAddresses.length; i++) {
            peerConnectionServices[i] = new LedgerPeerConnectionManager(ledger, peerAddresses[i], keyPair, certificate, credentialProvider, clientManager, this);
        }
        return new LedgerPeersManager(ledger, peerConnectionServices, keyPair, certificate, credentialProvider, clientManager, this, topologyStorage);
    }

    @Override
    public void close() {
        ledgersLock.writeLock().lock();
        try {
            for (LedgerPeersManager peersService : ledgerServices.values()) {
                try {
                    peersService.close();
                } catch (Exception e) {
                    logger.error("Close peersService {} error", peersService.getLedger(), e);
                }
            }
        } finally {
            ledgersLock.writeLock().unlock();
        }
    }
}
