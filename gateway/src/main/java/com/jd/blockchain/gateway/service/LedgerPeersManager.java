package com.jd.blockchain.gateway.service;

import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.NodeNetworkAddresses;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.gateway.service.topology.LedgerPeerApiServicesTopology;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopology;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopologyStorage;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.sdk.service.SessionCredentialProvider;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.net.NetworkAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 账本-共识节点管理
 */
public class LedgerPeersManager implements LedgerPeerConnectionListener {

    // query topology 定时周期，毫秒
    public static final int UPDATE_TOPOLOGY_INTERVAL = 15000;
    private static final Logger logger = LoggerFactory.getLogger(LedgerPeersManager.class);
    private ScheduledExecutorService executorService;

    // 账本哈希
    private HashDigest ledger;
    // 所连接节点地址等信息
    private AsymmetricKeypair keyPair;
    private SessionCredentialProvider credentialProvider;
    private ConsensusClientManager clientManager;
    private LedgersListener ledgersListener;
    private LedgerPeersTopologyStorage topologyStorage;
    private boolean topologyAware;
    // 连接列表
    private Map<NetworkAddress, LedgerPeerConnectionManager> connections;

    private ReadWriteLock connectionsLock = new ReentrantReadWriteLock();

    // 是否准备就绪，已经有可用连接
    private volatile boolean ready;

    public LedgerPeersManager(HashDigest ledger, LedgerPeerConnectionManager[] peerConnectionServices, AsymmetricKeypair keyPair,
                              SessionCredentialProvider credentialProvider, ConsensusClientManager clientManager,
                              LedgersListener ledgersListener, LedgerPeersTopologyStorage topologyStorage) {
        this(ledger, peerConnectionServices, keyPair, credentialProvider, clientManager, ledgersListener, topologyStorage, true);
    }

    public LedgerPeersManager(HashDigest ledger, LedgerPeerConnectionManager[] peerConnectionServices, AsymmetricKeypair keyPair,
                              SessionCredentialProvider credentialProvider, ConsensusClientManager clientManager,
                              LedgersListener ledgersListener, LedgerPeersTopologyStorage topologyStorage, boolean topologyAware) {
        this.ledger = ledger;
        this.keyPair = keyPair;
        this.credentialProvider = credentialProvider;
        this.clientManager = clientManager;
        this.ledgersListener = ledgersListener;
        this.topologyStorage = topologyStorage;
        this.topologyAware = topologyAware;
        executorService = Executors.newSingleThreadScheduledExecutor();
        this.connections = new ConcurrentHashMap<>();
        for (LedgerPeerConnectionManager manager : peerConnectionServices) {
            manager.setConnectionListener(this);
            connections.put(manager.getPeerAddress(), manager);
        }
    }

    public LedgerPeerConnectionManager newPeerConnectionManager(NetworkAddress peerAddress) {
        return new LedgerPeerConnectionManager(ledger, peerAddress, keyPair, credentialProvider, clientManager, ledgersListener);
    }

    public HashDigest getLedger() {
        return ledger;
    }

    public Set<NetworkAddress> getPeerAddresses() {
        connectionsLock.readLock().lock();
        try {
            return connections.keySet();
        } finally {
            connectionsLock.readLock().unlock();
        }
    }

    public BlockchainQueryService getQueryService() {
        connectionsLock.readLock().lock();
        try {
            long highestHeight = -1;
            Map<Long, Set<NetworkAddress>> connectionGroupByHeight = new HashMap<>();
            for (Map.Entry<NetworkAddress, LedgerPeerConnectionManager> entry : connections.entrySet()) {
                long height = entry.getValue().getLatestHeight();
                if (height > highestHeight) {
                    highestHeight = height;
                }
                if (!connectionGroupByHeight.containsKey(height)) {
                    connectionGroupByHeight.put(height, new HashSet<>());
                }
                connectionGroupByHeight.get(height).add(entry.getKey());
            }

            if (highestHeight > -1) {
                Set<NetworkAddress> selectedConnections = connectionGroupByHeight.get(highestHeight);
                return connections.get(new ArrayList(selectedConnections).get(new Random().nextInt(selectedConnections.size()))).getQueryService();
            }

            throw new IllegalStateException("No available query service for ledger: " + ledger);
        } finally {
            connectionsLock.readLock().unlock();
        }
    }

    public TransactionService getTransactionService() {
        connectionsLock.readLock().lock();
        try {
            long highestHeight = -1;
            Map<Long, Set<NetworkAddress>> connectionGroupByHeight = new HashMap<>();
            for (Map.Entry<NetworkAddress, LedgerPeerConnectionManager> entry : connections.entrySet()) {
                // 去除未认证连接
                if (!entry.getValue().isAuthorized()) {
                    continue;
                }
                long height = entry.getValue().getLatestHeight();
                if (height > highestHeight) {
                    highestHeight = height;
                }
                if (!connectionGroupByHeight.containsKey(height)) {
                    connectionGroupByHeight.put(height, new HashSet<>());
                }
                connectionGroupByHeight.get(height).add(entry.getKey());
            }

            if (highestHeight > -1) {
                Set<NetworkAddress> selectedConnections = connectionGroupByHeight.get(highestHeight);
                return connections.get(new ArrayList(selectedConnections).get(new Random().nextInt(selectedConnections.size()))).getTransactionService();
            }

            throw new IllegalStateException("No available tx service for ledger: " + ledger);
        } finally {
            connectionsLock.readLock().unlock();
        }
    }

    /**
     * 重置所有连接
     */
    public void reset() {
        logger.info("Ledger reset {}", ledger);
        clientManager.reset();
        updatePeers(connections.keySet(), true);
    }

    public void addPeer(NetworkAddress peer) {
        connectionsLock.writeLock().lock();
        try {
            if (connections.containsKey(peer)) {
                return;
            }
            logger.debug("Add peer {} in {}", peer, ledger);
            LedgerPeerConnectionManager connectionManager = newPeerConnectionManager(peer);
            connectionManager.setConnectionListener(this);
            connectionManager.startTimerTask();
            connections.put(peer, connectionManager);
        } finally {
            connectionsLock.writeLock().unlock();
        }
    }

    public synchronized void startTimerTask() {
        // 所有连接启动定时任务
        for (LedgerPeerConnectionManager manager : connections.values()) {
            manager.startTimerTask();
        }

        if(topologyAware) {
            // 启动定期拓扑感知
            executorService.scheduleWithFixedDelay(() -> updateTopologyTask(), 5000, UPDATE_TOPOLOGY_INTERVAL, TimeUnit.MILLISECONDS);
        } else {
            storeTopology(new LedgerPeerApiServicesTopology(ledger, keyPair, connections.keySet()));
        }
    }

    /**
     * 查询拓扑，更新共识节点连接
     */
    private void updateTopologyTask() {

        // 拓扑结构写入磁盘
        if (null != topologyStorage) {
            connectionsLock.readLock().lock();
            try {
                LedgerPeersTopology topology = new LedgerPeerApiServicesTopology(ledger, keyPair, connections.keySet());
                storeTopology(topology);
                logger.debug("Store topology {}", topology);
            } finally {
                connectionsLock.readLock().unlock();
            }
        }

        // 统计未认证连接数量，并关闭所有连接均为未认证的账本-连接拓扑管理
        int unAuthorizedSize = 0;
        connectionsLock.writeLock().lock();
        try {
            if (connections.size() >= 4) {
                for (Map.Entry<NetworkAddress, LedgerPeerConnectionManager> entry : connections.entrySet()) {
                    if (!entry.getValue().isAuthorized()) {
                        unAuthorizedSize++;
                    }
                }
                if (unAuthorizedSize == connections.size()) {
                    logger.info("Close ledger {}", ledger);
                    if (null != ledgersListener) {
                        // 移除账本
                        ledgersListener.LedgerRemoved(ledger);
                    }
                    storeTopology(new LedgerPeerApiServicesTopology(ledger, keyPair, new HashSet<>()));
                    close();
                    return;
                }
            }
        } finally {
            connectionsLock.writeLock().unlock();
        }

        Set<NetworkAddress> addresses = updateTopology();
        if (null != addresses) {
            // 更新连接
            updatePeers(addresses, false);
        }
    }

    public Set<NetworkAddress> updateTopology() {
        for (Map.Entry<NetworkAddress, LedgerPeerConnectionManager> entry : connections.entrySet()) {
            try {
                logger.debug("UpdateTopology by {}", entry.getKey());
                NodeNetworkAddresses nodeNetworkAddresses = entry.getValue().loadMonitors();
                if (null != nodeNetworkAddresses) {
                    NodeNetworkAddress[] nodeAddresses = nodeNetworkAddresses.getNodeNetworkAddresses();
                    if (nodeAddresses != null && nodeAddresses.length > 0) {
                        Set<NetworkAddress> addresses = new HashSet<>();
                        boolean satisfied = true;
                        for (NodeNetworkAddress address : nodeAddresses) {
                            // 存在端口小于0的情况则不使用此次查询结果
                            if (address.getMonitorPort() > 0) {
                                addresses.add(new NetworkAddress(address.getHost(), address.getMonitorPort()));
                            } else {
                                satisfied = false;
                                break;
                            }
                        }
                        if (satisfied) {
                            logger.debug("UpdateTopology by {} : {}", entry.getKey(), addresses);
                            return addresses;
                        } else {
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("UpdateTopology by {}", entry.getKey(), e);
            }
        }

        return null;
    }

    private void storeTopology(LedgerPeersTopology topology) {
        if (null != topologyStorage) {
            try {
                topologyStorage.setTopology(ledger.toBase58(), topology);
                logger.debug("Store topology {}", topology);
            } catch (Exception e) {
                logger.error("Store topology error", e);
            }
        }
    }

    /**
     * 更新节点信息
     *
     * @param peers 待更新地址列表
     * @param force 是否强制更新，true会根据传入peers创建并替换现有连接，false则会只在匹配有地址更新时替换。
     */
    private void updatePeers(Set<NetworkAddress> peers, boolean force) {
        connectionsLock.writeLock().lock();
        try {
            if (!force) {
                if (null == peers || peers.size() == 0) {
                    return;
                }
                // 判断连接拓扑是否有变化
                if (peers.size() == connections.size() && connections.keySet().containsAll(peers)) {
                    return;
                }
            }

            closeConsensusClient();

            // 关闭旧的连接，替换新连接
            Map<NetworkAddress, LedgerPeerConnectionManager> oldConnections = new HashMap<>();
            oldConnections.putAll(connections);
            for (Map.Entry<NetworkAddress, LedgerPeerConnectionManager> entry : oldConnections.entrySet()) {
                if (null != entry.getValue()) {
                    entry.getValue().close();
                }
            }

            // 有差异，重新创建所有连接
            Map<NetworkAddress, LedgerPeerConnectionManager> newConnections = new ConcurrentHashMap<>();
            for (NetworkAddress address : peers) {
                LedgerPeerConnectionManager connectionManager = newPeerConnectionManager(address);
                connectionManager.setConnectionListener(this);
                connectionManager.startTimerTask();
                newConnections.put(address, connectionManager);
            }

            connections = newConnections;
        } catch (Exception e) {
            logger.error("UpdateTopology {}:{}", ledger, peers, e);
        } finally {
            connectionsLock.writeLock().unlock();
        }
    }

    public void close() {
        connectionsLock.writeLock().lock();
        try {

            closeConsensusClient();

            for (Map.Entry<NetworkAddress, LedgerPeerConnectionManager> entry : connections.entrySet()) {
                entry.getValue().close();
            }

            executorService.shutdownNow();

            logger.info("LedgerManager {} closed", ledger);
        } finally {
            connectionsLock.writeLock().unlock();
        }
    }

    @Override
    public void connected(NetworkAddress peer) {
        logger.info("LedgerManager {} is ready", ledger);
        this.ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * 关闭 consensus client
     */
    private void closeConsensusClient() {
        logger.info("Close consensus client for {}", getLedger());
        clientManager.remove(getLedger());
    }
}
