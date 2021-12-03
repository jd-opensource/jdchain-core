package com.jd.blockchain.consensus.raft.client;

import com.alipay.sofa.jraft.JRaftUtils;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.CliRequests;
import com.alipay.sofa.jraft.rpc.RpcResponseClosureAdapter;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.protobuf.ProtocolStringList;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.NodeNetworkAddresses;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.manage.ConsensusManageService;
import com.jd.blockchain.consensus.manage.ConsensusView;
import com.jd.blockchain.consensus.raft.config.RaftReplica;
import com.jd.blockchain.consensus.raft.manager.RaftConsensusView;
import com.jd.blockchain.consensus.raft.rpc.QueryManagerInfoRequest;
import com.jd.blockchain.consensus.raft.rpc.QueryManagerInfoRequestProcessor;
import com.jd.blockchain.consensus.raft.rpc.RpcResponse;
import com.jd.blockchain.consensus.raft.rpc.SubmitTxRequest;
import com.jd.blockchain.consensus.raft.settings.RaftClientSettings;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNetworkSettings;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.consensus.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;
import utils.net.NetworkAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class RaftMessageService implements MessageService, ConsensusManageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftMessageService.class);
    private static final int MAX_RETRY_TIMES = 3;

    private String groupId;
    private PeerId leader;
    private long lastLeaderUpdateTimestamp;
    private ReentrantLock lock = new ReentrantLock();
    private ScheduledExecutorService refreshLeaderExecutorService;
    private Executor monitorExecutor;

    private CliClientServiceImpl clientService;
    private Configuration configuration;

    private final int rpcTimeoutMs;
    private final int refreshLeaderMs;

    public RaftMessageService(String groupId, RaftClientSettings settings) {

        RaftConsensusSettings consensusSettings = (RaftConsensusSettings) settings.getViewSettings();
        RaftNetworkSettings networkSettings = consensusSettings.getNetworkSettings();

        this.rpcTimeoutMs = networkSettings.getRpcRequestTimeoutMs();
        this.refreshLeaderMs = consensusSettings.getElectionTimeoutMs();

        CliOptions cliOptions = new CliOptions();
        cliOptions.setRpcConnectTimeoutMs(networkSettings.getRpcConnectTimeoutMs());
        cliOptions.setRpcDefaultTimeout(networkSettings.getRpcDefaultTimeoutMs());
        cliOptions.setRpcInstallSnapshotTimeout(networkSettings.getRpcSnapshotTimeoutMs());
        cliOptions.setTimeoutMs(networkSettings.getRpcRequestTimeoutMs());
        cliOptions.setMaxRetry(MAX_RETRY_TIMES);

        this.clientService = new CliClientServiceImpl();
        this.clientService.init(cliOptions);

        this.groupId = groupId;
        this.configuration = new Configuration();
        for (String currentPeer : settings.getCurrentPeers()) {
            this.configuration.addPeer(PeerId.parsePeer(currentPeer));
        }

        this.refreshLeaderExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.monitorExecutor = JRaftUtils.createExecutor("RAFT-CLIENT-MONITOR", Runtime.getRuntime().availableProcessors());
    }

    public void init() {
        RouteTable.getInstance().updateConfiguration(groupId, configuration);
        refresh();
        refreshLeaderExecutorService.scheduleAtFixedRate(this::refresh, refreshLeaderMs, refreshLeaderMs, TimeUnit.MILLISECONDS);
    }

    private void refresh() {
        lock.lock();
        try {
            if (System.currentTimeMillis() - this.lastLeaderUpdateTimestamp < refreshLeaderMs) {
                return;
            }

            RouteTable.getInstance().refreshLeader(this.clientService, this.groupId, this.rpcTimeoutMs);
            RouteTable.getInstance().refreshConfiguration(this.clientService, this.groupId, this.rpcTimeoutMs);

            PeerId peerId = RouteTable.getInstance().selectLeader(this.groupId);
            if (peerId != null && !peerId.equals(this.leader)) {
                LoggerUtils.infoIfEnabled(LOGGER, "leader changed. from {} to {}", this.leader, peerId);
                this.leader = peerId;
                this.lastLeaderUpdateTimestamp = System.currentTimeMillis();
            }
        } catch (Exception e) {
            LOGGER.error("refresh raft client config error", e);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public AsyncFuture<byte[]> sendOrdered(byte[] message) {
        ensureConnected();
        CompletableAsyncFuture<byte[]> asyncFuture = new CompletableAsyncFuture<>();
        try {
            int retry = 0;
            SubmitTxRequest txRequest = new SubmitTxRequest(message);
            sendRequest(leader.getEndpoint(), txRequest, retry, asyncFuture);
        } catch (Exception e) {
            throw new RaftClientRequestException(e);
        }
        return asyncFuture;
    }

    private void sendRequest(Endpoint endpoint, SubmitTxRequest txRequest, int retry, CompletableAsyncFuture<byte[]> asyncFuture)
            throws RemotingException, InterruptedException {

        if (retry >= MAX_RETRY_TIMES) {
            asyncFuture.error(new RuntimeException("raft client send request exceed max retries"));
            return;
        }

        if (endpoint == null) {
            asyncFuture.error(new RuntimeException("raft client send request find leader endpoint is null"));
            return;
        }

        clientService.getRpcClient().invokeAsync(endpoint, txRequest, (o, throwable) -> {

            LoggerUtils.debugIfEnabled(LOGGER, "raft client send request: {} response: {} throwable: {}", txRequest, o, throwable);

            if (throwable != null) {
                LOGGER.error("raft client send request error, request: {}", txRequest, throwable);
                asyncFuture.error(throwable);
                return;
            }

            RpcResponse response = (RpcResponse) o;
            if (response.isRedirect()) {
                LoggerUtils.debugIfEnabled(LOGGER, "request should redirect to leader. current peer: {} , redirect leader: {}", endpoint, response.getLeaderEndpoint());
                try {
                    sendRequest(JRaftUtils.getEndPoint(response.getLeaderEndpoint()), txRequest, retry + 1, asyncFuture);
                } catch (Exception e) {
                    throw new RaftClientRequestException(e);
                }
                return;
            }

            if (response.isSuccess()) {
                asyncFuture.complete(response.getResult());
            } else {
                asyncFuture.complete(null);
            }

        }, rpcTimeoutMs);
    }


    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Client has not connected to the leader nodes!");
        }
    }

    @Override
    public AsyncFuture<byte[]> sendUnordered(byte[] message) {
        ensureConnected();
        if (Arrays.equals(MonitorService.LOAD_MONITOR, message)) {
            return refreshAndQueryPeersManagerInfo();
        }
        return new CompletableAsyncFuture<>();
    }

    private AsyncFuture<byte[]> refreshAndQueryPeersManagerInfo() {
        refresh();
        Configuration configuration = RouteTable.getInstance().getConfiguration(this.groupId);
        List<PeerId> peerIds = configuration.listPeers();

        return CompletableAsyncFuture.callAsync((Callable<byte[]>) () -> {
            try {
                List<MonitorNodeNetwork> monitorNodeNetworkList = queryPeersManagerInfo(peerIds);
                MonitorNodeNetwork[] monitorNodeNetworks = monitorNodeNetworkList.toArray(new MonitorNodeNetwork[]{});
                MonitorNodeNetworkAddresses addresses = new MonitorNodeNetworkAddresses(monitorNodeNetworks);
                return BinaryProtocol.encode(addresses, NodeNetworkAddresses.class);
            } catch (Exception e) {
                LOGGER.error("refreshAndQueryPeersManagerInfo error", e);
            }
            return null;
        }, monitorExecutor);
    }

    private List<MonitorNodeNetwork> queryPeersManagerInfo(List<PeerId> peerIds) throws InterruptedException {

        final List<MonitorNodeNetwork> nodeNetworkList = new ArrayList<>(peerIds.size());
        CountDownLatch countDownLatch = new CountDownLatch(peerIds.size());
        QueryManagerInfoRequest infoRequest = new QueryManagerInfoRequest();

        for (PeerId peerId : peerIds) {
            try {
                clientService.getRpcClient().invokeAsync(peerId.getEndpoint(), infoRequest, (o, e) -> {
                    try {
                        if (e != null) {
                            LoggerUtils.errorIfEnabled(LOGGER, "queryPeersManagerInfo error, peer id: {}", peerId, e);
                        } else {
                            RpcResponse response = (RpcResponse) o;
                            QueryManagerInfoRequestProcessor.ManagerInfoResponse managerInfoResponse =
                                    QueryManagerInfoRequestProcessor.ManagerInfoResponse.fromBytes(response.getResult());

                            MonitorNodeNetwork monitorNodeNetwork = new MonitorNodeNetwork(
                                    managerInfoResponse.getHost(),
                                    managerInfoResponse.getConsensusPort(),
                                    managerInfoResponse.getManagerPort(),
                                    managerInfoResponse.isConsensusSSLEnabled(),
                                    managerInfoResponse.isManagerSSLEnabled()
                            );

                            nodeNetworkList.add(monitorNodeNetwork);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }, this.rpcTimeoutMs);
            } catch (Exception e) {
                countDownLatch.countDown();
            }
        }

        countDownLatch.await(this.rpcTimeoutMs * peerIds.size(), TimeUnit.MILLISECONDS);
        return nodeNetworkList;
    }


    @Override
    public AsyncFuture<ConsensusView> addNode(Replica replica) {
        ensureConnected();
        CompletableAsyncFuture<ConsensusView> asyncFuture = new CompletableAsyncFuture<>();

        RaftReplica raftReplica = (RaftReplica) replica;
        CliRequests.AddPeerRequest addPeerRequest = CliRequests.AddPeerRequest.newBuilder()
                .setGroupId(this.groupId)
                .setPeerId(raftReplica.getPeerStr())
                .build();

        clientService.addPeer(leader.getEndpoint(), addPeerRequest, new RpcResponseClosureAdapter<CliRequests.AddPeerResponse>() {
            @Override
            public void run(Status status) {
                CliRequests.AddPeerResponse response = getResponse();
                LoggerUtils.debugIfEnabled(LOGGER, "raft client add node {} result is: {}, response is: {}", replica, status, response);

                if (!status.isOk()) {
                    asyncFuture.error(new RuntimeException(status.getErrorMsg()));
                    return;
                }

                RaftConsensusView consensusView = new RaftConsensusView();
                consensusView.setOldPeers(covertToRaftNodeInfoArray(response.getOldPeersList()));
                consensusView.setNewPeers(covertToRaftNodeInfoArray(response.getNewPeersList()));

                asyncFuture.complete(consensusView);
            }

        });

        return asyncFuture;
    }

    private RaftConsensusView.RaftNodeInfo[] covertToRaftNodeInfoArray(ProtocolStringList peersList) {

        if (peersList == null || peersList.asByteStringList().isEmpty()) {
            return null;
        }

        return peersList.asByteStringList().stream()
                .map(x -> PeerId.parsePeer(x.toStringUtf8()))
                .map(p -> new RaftConsensusView.RaftNodeInfo(0, new NetworkAddress(p.getIp(), p.getPort())))
                .toArray(RaftConsensusView.RaftNodeInfo[]::new);
    }

    @Override
    public AsyncFuture<ConsensusView> removeNode(Replica replica) {
        ensureConnected();
        CompletableAsyncFuture<ConsensusView> asyncFuture = new CompletableAsyncFuture<>();

        RaftReplica raftReplica = (RaftReplica) replica;
        CliRequests.RemovePeerRequest removePeerRequest = CliRequests.RemovePeerRequest.newBuilder()
                .setGroupId(this.groupId)
                .setPeerId(raftReplica.getPeerStr())
                .build();

        clientService.removePeer(leader.getEndpoint(), removePeerRequest, new RpcResponseClosureAdapter<CliRequests.RemovePeerResponse>() {
            @Override
            public void run(Status status) {
                CliRequests.RemovePeerResponse response = getResponse();
                LoggerUtils.debugIfEnabled(LOGGER, "raft client remove node {} result is: {}, response is: {}", replica, status, response);

                if (!status.isOk()) {
                    asyncFuture.error(new RuntimeException(status.getErrorMsg()));
                    return;
                }

                RaftConsensusView consensusView = new RaftConsensusView();
                consensusView.setOldPeers(covertToRaftNodeInfoArray(response.getOldPeersList()));
                consensusView.setNewPeers(covertToRaftNodeInfoArray(response.getNewPeersList()));

                asyncFuture.complete(consensusView);
            }


        });

        return asyncFuture;
    }

    public void close() {
        if (this.refreshLeaderExecutorService != null) {
            this.refreshLeaderExecutorService.shutdownNow();
        }

        if (this.clientService != null) {
            this.clientService.shutdown();
        }
    }

    public boolean isConnected() {
        return this.leader != null && clientService.isConnected(this.leader.getEndpoint());
    }

    static class MonitorNodeNetworkAddresses implements NodeNetworkAddresses {
        private NodeNetworkAddress[] nodeNetworkAddresses = null;

        public MonitorNodeNetworkAddresses() {
        }

        public MonitorNodeNetworkAddresses(NodeNetworkAddress[] nodeNetworkAddresses) {
            this.nodeNetworkAddresses = nodeNetworkAddresses;
        }

        @Override
        public NodeNetworkAddress[] getNodeNetworkAddresses() {
            return nodeNetworkAddresses;
        }
    }

    static class MonitorNodeNetwork implements NodeNetworkAddress {

        String host;
        int consensusPort;
        int monitorPort;
        boolean consensusSecure;
        boolean monitorSecure;

        public MonitorNodeNetwork() {
        }

        public MonitorNodeNetwork(String host, int consensusPort, int monitorPort, boolean consensusSecure, boolean monitorSecure) {
            this.host = host;
            this.consensusPort = consensusPort;
            this.monitorPort = monitorPort;
            this.consensusSecure = consensusSecure;
            this.monitorSecure = monitorSecure;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getConsensusPort() {
            return consensusPort;
        }

        @Override
        public int getMonitorPort() {
            return monitorPort;
        }

        @Override
        public boolean isConsensusSecure() {
            return consensusSecure;
        }

        @Override
        public boolean isMonitorSecure() {
            return monitorSecure;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setConsensusPort(int consensusPort) {
            this.consensusPort = consensusPort;
        }

        public void setMonitorPort(int monitorPort) {
            this.monitorPort = monitorPort;
        }

        public void setConsensusSecure(boolean consensusSecure) {
            this.consensusSecure = consensusSecure;
        }

        public void setMonitorSecure(boolean monitorSecure) {
            this.monitorSecure = monitorSecure;
        }
    }

}
