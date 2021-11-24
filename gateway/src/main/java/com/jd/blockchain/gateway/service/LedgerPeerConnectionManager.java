package com.jd.blockchain.gateway.service;

import com.jd.blockchain.consensus.NodeNetworkAddresses;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.sdk.service.PeerAuthenticator;
import com.jd.blockchain.sdk.service.PeerBlockchainServiceFactory;
import com.jd.blockchain.sdk.service.SessionCredentialProvider;
import com.jd.blockchain.setting.GatewayAuthResponse;
import com.jd.blockchain.setting.LedgerIncomingSettings;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.net.NetworkAddress;
import utils.net.SSLSecurity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 共识节点连接
 */
public class LedgerPeerConnectionManager {
    // ping 定时周期，秒
    public static final int PING_INTERVAL = 2000;
    // 认证 定时周期，秒
    public static final int AUTH_INTERVAL = 5000;
    private static final Logger logger = LoggerFactory.getLogger(LedgerPeerConnectionManager.class);
    private ScheduledExecutorService executorService;

    // 账本
    private HashDigest ledger;
    // 所连接节点地址等信息
    private NetworkAddress peerAddress;
    private AsymmetricKeypair keyPair;
    private SessionCredentialProvider credentialProvider;
    private ConsensusClientManager clientManager;
    private LedgerPeerConnectionListener connectionListener;
    // 是否有效
    private volatile State state;
    // 连接工厂
    private volatile PeerBlockchainServiceFactory blockchainServiceFactory;
    // 账本变化监听
    private LedgersListener ledgersListener;
    // 最新区块高度
    private volatile long latestHeight;
    // 可访问的账本列表
    private Set<HashDigest> accessibleLedgers;

    private PeerAuthenticator authenticator;
    private SSLSecurity manageSslSecurity;
    private SSLSecurity consensusSslSecurity;

    public LedgerPeerConnectionManager(HashDigest ledger, NetworkAddress peerAddress, AsymmetricKeypair keyPair,
                                       SessionCredentialProvider credentialProvider, ConsensusClientManager clientManager,
                                       LedgersListener ledgersListener) {
        this(ledger, peerAddress, new SSLSecurity(), new SSLSecurity(), keyPair, credentialProvider, clientManager, ledgersListener);
    }

    public LedgerPeerConnectionManager(HashDigest ledger, NetworkAddress peerAddress, SSLSecurity manageSslSecurity,
                                       SSLSecurity consensusSslSecurity, AsymmetricKeypair keyPair, SessionCredentialProvider credentialProvider,
                                       ConsensusClientManager clientManager, LedgersListener ledgersListener) {
        this.executorService = Executors.newScheduledThreadPool(2);
        this.latestHeight = -1;
        this.state = State.UNAVAILABLE;
        this.manageSslSecurity = manageSslSecurity;
        this.consensusSslSecurity = consensusSslSecurity;
        this.ledger = ledger;
        this.accessibleLedgers = new HashSet<>();
        this.accessibleLedgers.add(ledger);
        this.peerAddress = peerAddress;
        this.keyPair = keyPair;
        this.credentialProvider = credentialProvider;
        this.clientManager = clientManager;
        this.ledgersListener = ledgersListener;
        this.authenticator = new PeerAuthenticator(peerAddress, manageSslSecurity, keyPair, credentialProvider);
    }

    public void setConnectionListener(LedgerPeerConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public HashDigest getLedger() {
        return ledger;
    }

    public NetworkAddress getPeerAddress() {
        return peerAddress;
    }

    public BlockchainQueryService getQueryService() {
        if (null != blockchainServiceFactory) {
            return blockchainServiceFactory.getBlockchainService();
        }

        return null;
    }

    public boolean connected() {
        return null != blockchainServiceFactory;
    }

    public TransactionService getTransactionService() {
        if (null != blockchainServiceFactory) {
            return blockchainServiceFactory.getTransactionService();
        }

        return null;
    }

    public NodeNetworkAddresses loadMonitors() {
        return connected() ? blockchainServiceFactory.getMonitorServiceMap().get(ledger).loadMonitors() : null;
    }

    public synchronized void startTimerTask() {
        int randomDelay = new Random().nextInt(500);
        // 启动有效性检测或重连
        executorService.scheduleWithFixedDelay(() -> {
            try {
                if (connected()) {
                    pingTask();
                } else {
                    connectTask();
                }
            } catch (Exception e) {
                logger.error("Ping or Reconnect {}-{} error", ledger, peerAddress, e);
            }
        }, randomDelay, PING_INTERVAL, TimeUnit.MILLISECONDS);

        // 认证线程
        executorService.scheduleWithFixedDelay(() -> {
            if (connected()) {
                authTask();
            }
        }, 2 * randomDelay, AUTH_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 当前连接是否可用
     *
     * @return
     */
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    /**
     * 当前连接是否不再对账本有访问权限
     *
     * @return
     */
    public boolean isAuthorized() {
        return state != State.UnAuthorized;
    }

    /**
     * 最新区块高度
     *
     * @return 最新区块高度，处于不可用状态时返回-1
     */
    public long getLatestHeight() {
        if (isAvailable()) {
            return latestHeight;
        } else {
            return -1;
        }
    }

    public synchronized void close() {
        try {
            executorService.shutdownNow();
            if (null != blockchainServiceFactory) {
                blockchainServiceFactory.close();
            }
            blockchainServiceFactory = null;
            connectionListener = null;
            ledgersListener = null;
            logger.info("Shutdown {}:{}", ledger, peerAddress);
        } catch (Exception e) {
            logger.error("Shutdown {}:{}", ledger, peerAddress, e);
        }
    }

    /**
     * 连接
     */
    private synchronized void connectTask() {
        logger.debug("Connect {}-{}", ledger, peerAddress);
        try {
            Set<HashDigest> ledgers = new HashSet<>(Arrays.asList(connect()));
            logger.info("Connect {}-{}:{}", ledger, peerAddress, ledgers);
            if (ledgers.contains(ledger)) {
                state = State.AVAILABLE;
                if (null != connectionListener) {
                    connectionListener.connected(peerAddress);
                }
            } else {
                state = State.UnAuthorized;
            }

            // 触发账本变化处理
            if (accessibleLedgers.size() != ledgers.size() || !accessibleLedgers.containsAll(ledgers)) {
                notifyLedgersChange(ledgers);
                accessibleLedgers = ledgers;
            }

        } catch (Exception e) {
            state = State.UNAVAILABLE;
            logger.error("Connect {}-{} error", ledger, peerAddress, e);
        }
    }

    public synchronized HashDigest[] connect() {
        PeerBlockchainServiceFactory factory = PeerBlockchainServiceFactory.connect(keyPair, peerAddress, manageSslSecurity, consensusSslSecurity, credentialProvider, clientManager);
        if (null != blockchainServiceFactory) {
            blockchainServiceFactory.close();
        }
        blockchainServiceFactory = factory;
        return factory.getLedgerHashs();
    }

    /**
     * 认证
     */
    private synchronized void authTask() {
        logger.debug("Auth {}-{}", ledger, peerAddress);
        try {
            GatewayAuthResponse authResponse = auth();
            Set<HashDigest> ledgers = Arrays.stream(authResponse.getLedgers()).map(LedgerIncomingSettings::getLedgerHash).collect(Collectors.toSet());
            if (ledgers.contains(ledger)) {
                state = State.AVAILABLE;
            } else {
                logger.warn("Auth {}-{} response no ledger", ledger, peerAddress);
                state = State.UnAuthorized;
            }

            if (accessibleLedgers.size() != ledgers.size() || !accessibleLedgers.containsAll(ledgers)) {
                if (isAvailable()) {
                    // 重新建立连接信息
                    try {
                        logger.info("Auth {}-{} recreate connection", ledger, peerAddress);
                        blockchainServiceFactory.close();
                        blockchainServiceFactory = PeerBlockchainServiceFactory.create(keyPair, peerAddress, manageSslSecurity, consensusSslSecurity, authResponse.getLedgers(), credentialProvider, clientManager);
                    } catch (Exception e) {
                        logger.warn("Auth {}-{} recreate connection", ledger, peerAddress, e);
                    }
                }

                // 触发账本变化处理
                notifyLedgersChange(ledgers);
                accessibleLedgers = ledgers;
            }

        } catch (Exception e) {
            state = State.UNAVAILABLE;
            logger.error("Auth {}-{} error", ledger, peerAddress, e);
        }
    }

    public GatewayAuthResponse auth() {
        return authenticator.request();
    }

    /**
     * 账本变化处理
     *
     * @param ledgers
     */
    private void notifyLedgersChange(Set<HashDigest> ledgers) {
        Set<HashDigest> ledgersToAdd = new HashSet<>();
        ledgersToAdd.addAll(ledgers);
        ledgersToAdd.remove(ledger);
        if (null != ledgersListener && ledgersToAdd.size() > 0) {
            logger.info("Ledgers update {}->{}, only for adding", accessibleLedgers, ledgers);
            executorService.execute(() -> {
                ledgersListener.LedgersUpdated(ledgersToAdd, keyPair, peerAddress);
            });
        }
    }

    /**
     * 有效性检测
     * <p>
     * 通过查询账本信息检测有效性，并更新最新区块高度信息
     */
    private synchronized void pingTask() {
        if (isAuthorized()) {
            logger.debug("Ping {}-{}", ledger, peerAddress);
            try {
                latestHeight = ping();
                state = latestHeight >= 0 ? State.AVAILABLE : State.UNAVAILABLE;
            } catch (Exception e) {
                latestHeight = -1;
                state = State.UNAVAILABLE;
                logger.error("Ping {}-{} error", ledger, peerAddress, e);
            }
        }
    }

    public long ping() {
        return blockchainServiceFactory.getBlockchainService().getLedger(ledger).getLatestBlockHeight();
    }

    /**
     * 节点连接状态
     */
    public enum State {
        AVAILABLE, // 有效
        UNAVAILABLE, // 不可用
        UnAuthorized // 未认证
    }

}
