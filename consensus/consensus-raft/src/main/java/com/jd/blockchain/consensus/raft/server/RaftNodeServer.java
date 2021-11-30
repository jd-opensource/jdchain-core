package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.*;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.CliServiceImpl;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.google.common.io.Files;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.RaftConsensusProvider;
import com.jd.blockchain.consensus.raft.config.RaftConfig;
import com.jd.blockchain.consensus.raft.consensus.BlockSerializer;
import com.jd.blockchain.consensus.raft.rpc.ParticipantNodeChangeRequestProcessor;
import com.jd.blockchain.consensus.raft.rpc.SubmitTxRequestProcessor;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.consensus.raft.settings.RaftServerSettings;
import com.jd.blockchain.consensus.raft.spring.LedgerManageUtils;
import com.jd.blockchain.consensus.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

import java.io.File;

public class RaftNodeServer implements NodeServer {

    static {
        System.getProperties().setProperty("bolt.netty.buffer.low.watermark", String.valueOf(128 * 1024 * 1024));
        System.getProperties().setProperty("bolt.netty.buffer.high.watermark", String.valueOf(512 * 1024 * 1024));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftNodeServer.class);

    private String realmName;
    private PeerId selfPeerId;
    private RaftServerSettings serverSettings;
    private MessageHandle messageHandle;
    private RaftClientAuthenticationService clientAuthencationService;
    private BlockSerializer blockSerializer;

    private Configuration configuration;
    private NodeOptions nodeOptions;

    private Node raftNode;
    private RpcServer rpcServer;
    private RpcClient rpcClient;
    private CliClientServiceImpl raftClientService;

    private RaftGroupService raftGroupService;
    private RaftNodeServerService raftNodeServerService;

    private volatile boolean isStart;
    private volatile boolean isStop;


//    private ScheduledExecutorService refreshRouteTableExecutor =  Executors.newScheduledThreadPool(1);

    public RaftNodeServer(String realmName, RaftServerSettings serverSettings, MessageHandle messageHandler) {
        this.realmName = realmName;
        this.serverSettings = serverSettings;
        this.messageHandle = messageHandler;
        init(this.serverSettings);
    }

    private void init(RaftServerSettings raftServerSettings) {
        this.selfPeerId = nodeSettingsToPeerId(raftServerSettings.getReplicaSettings());
        this.configuration = new Configuration();

        for (NodeSettings nodeSettings : raftServerSettings.getConsensusSettings().getNodes()) {
            this.configuration.addPeer(nodeSettingsToPeerId(nodeSettings));
        }

        if (!this.configuration.contains(selfPeerId)) {
            this.configuration.addPeer(selfPeerId);
        }

        this.nodeOptions = initNodeOptions(raftServerSettings);
        this.nodeOptions.setInitialConf(this.configuration);

        RaftConsensusStateMachine stateMachine = new RaftConsensusStateMachine();
        stateMachine.setCommitter(new BlockCommitService(this.realmName, this.messageHandle, LedgerManageUtils.getLedgerManager()));
        stateMachine.setRaftNodeServer(this);
        this.nodeOptions.setFsm(stateMachine);

        this.clientAuthencationService = new RaftClientAuthenticationService(this);
        this.blockSerializer = new SimpleBlockSerializerService();
    }

    private PeerId nodeSettingsToPeerId(NodeSettings nodeSettings) {
        RaftNodeSettings raftNodeSettings = (RaftNodeSettings) nodeSettings;
        return new PeerId(raftNodeSettings.getNetworkAddress().getHost(), raftNodeSettings.getNetworkAddress().getPort());
    }

    private NodeOptions initNodeOptions(RaftServerSettings config) {
        NodeOptions options = new NodeOptions();

        options.setElectionTimeoutMs(config.getElectionTimeoutMs());
        options.setSnapshotIntervalSecs(config.getSnapshotIntervalSec());

        mkdirAndSetupDirs(config.getRaftNodeSettings().getRaftPath(), options);

        options.setSharedElectionTimer(true);
        options.setSharedVoteTimer(true);
        options.setSharedStepDownTimer(true);
        options.setSharedSnapshotTimer(true);

        options.setRaftOptions(RaftConfig.buildRaftOptions(config.getRaftSettings()));

        return options;
    }


    public void stop() {
        if (isStop) {
            return;
        }

        isStop = true;
        this.raftNodeServerService.publishBlockEvent();

        if (raftClientService != null) {
            raftClientService.shutdown();
        }

//        refreshRouteTableExecutor.shutdown();

        if (raftGroupService != null) {
            raftGroupService.shutdown();
        }
    }


    public AsyncFuture start() {

        CompletableAsyncFuture completableAsyncFuture = new CompletableAsyncFuture();
        completableAsyncFuture.complete(null);

        if (isStart) {
            return completableAsyncFuture;
        }
        isStart = true;

        NodeManager manager = NodeManager.getInstance();
        this.configuration.listPeers().forEach(p -> manager.addAddress(p.getEndpoint()));

        this.raftGroupService = new RaftGroupService(this.realmName, selfPeerId, nodeOptions);
        this.raftNode = this.raftGroupService.start();

        this.raftNodeServerService = new RaftNodeServerServiceImpl(this);
        register(this.raftGroupService.getRpcServer());

        CliOptions cliOptions = new CliOptions();
        cliOptions.setRpcConnectTimeoutMs(this.serverSettings.getRaftNetworkSettings().getRpcConnectTimeoutMs());
        cliOptions.setRpcDefaultTimeout(this.serverSettings.getRaftNetworkSettings().getRpcDefaultTimeoutMs());
        cliOptions.setRpcInstallSnapshotTimeout(this.serverSettings.getRaftNetworkSettings().getRpcSnapshotTimeoutMs());

        this.raftClientService = (CliClientServiceImpl) ((CliServiceImpl) RaftServiceFactory.createAndInitCliService(cliOptions)).getCliClientService();
        this.rpcClient = this.raftClientService.getRpcClient();

        RouteTable.getInstance().updateConfiguration(this.realmName, this.configuration);
//        refreshRouteTableExecutor.scheduleWithFixedDelay(
//                this::refreshRouteTable,
//                this.config.getElectionTimeoutMs(),
//                this.config.getElectionTimeoutMs(),
//                TimeUnit.MILLISECONDS
//        );

        LOGGER.info("node: {} is started", this.raftNode.getNodeId());
        return completableAsyncFuture;
    }

    public boolean isLeader() {
        return raftNode.isLeader();
    }

    public PeerId getLeader() {
        return raftNode.getLeaderId();
    }


    private void register(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
        rpcServer.registerProcessor(new SubmitTxRequestProcessor(this.raftNodeServerService));
        rpcServer.registerProcessor(new ParticipantNodeChangeRequestProcessor(this.raftNodeServerService));
    }


    private void refreshRouteTable() {

        if (this.isStop) {
            return;
        }

        try {
            RouteTable routeTable = RouteTable.getInstance();
            Status status = routeTable.refreshLeader(this.raftClientService, this.realmName, this.serverSettings.getRaftNetworkSettings().getRpcRequestTimeoutMs());
            if (!status.isOk()) {
                LOGGER.warn("peer:{} refresh leader error: {}", selfPeerId, status.getErrorMsg());
            }
            status = RouteTable.getInstance().refreshConfiguration(this.raftClientService, this.realmName, this.serverSettings.getRaftNetworkSettings().getRpcRequestTimeoutMs());
            if (!status.isOk()) {
                LOGGER.warn("peer:{} refresh leader error: {}", selfPeerId, status.getErrorMsg());
            }
        } catch (Exception e) {
            LOGGER.error("refresh route table error", e);
        }
    }

    @Override
    public String getProviderName() {
        return RaftConsensusProvider.PROVIDER_NAME;
    }


    @Override
    public RaftServerSettings getServerSettings() {
        return serverSettings;
    }

    @Override
    public ClientAuthencationService getClientAuthencationService() {
        return clientAuthencationService;
    }


    @Override
    public NodeState getState() {
        //todo
        throw new IllegalStateException("Not implemented!");
    }


    @Override
    public Communication getCommunication() {
        //todo
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public boolean isRunning() {
        return isStart;
    }

    public String getRealmName() {
        return realmName;
    }

    public MessageHandle getMessageHandle() {
        return messageHandle;
    }

    public Node getNode() {
        return this.raftNode;
    }

    public RpcClient getRpcClient() {
        return this.rpcClient;
    }

    public BlockSerializer getTxSerializer() {
        return blockSerializer;
    }

    public String[] getCurrentPeerEndpoints(){
        //todo
        return null;
    }

    private void mkdirAndSetupDirs(String raftPath, NodeOptions nodeOptions) {
        try {

            String logPath = raftPath + File.separator + "log";
            String metaPath = raftPath + File.separator + "meta";
            String snapshotPath = raftPath + File.separator + "snapshot";

            Files.createParentDirs(new File(logPath));
            Files.createParentDirs(new File(metaPath));
            Files.createParentDirs(new File(snapshotPath));

            nodeOptions.setLogUri(logPath);
            nodeOptions.setRaftMetaUri(metaPath);
            nodeOptions.setSnapshotUri(snapshotPath);

        } catch (Exception e) {
            LOGGER.error("init dir error", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
