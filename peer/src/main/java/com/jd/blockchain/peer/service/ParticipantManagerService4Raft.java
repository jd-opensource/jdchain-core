package com.jd.blockchain.peer.service;

import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.util.Endpoint;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.rpc.*;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNetworkSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.peer.web.ManagementController;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.httpservice.utils.web.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import utils.Property;
import utils.StringUtils;
import utils.crypto.sm.GmSSLProvider;
import utils.net.NetworkAddress;
import utils.net.SSLSecurity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Component(ConsensusTypeEnum.RAFT_PROVIDER)
public class ParticipantManagerService4Raft implements IParticipantManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantManagerService4Raft.class);

    private static final int MAX_RETRY_TIMES = 3;
    private static final String RPC_QUEST_TIMEOUT_MS = "rpc_quest_timeout_ms";
    private static final String RPC_CLIENT = "rpc_client";
    private static final int SLEEP_MS = 2 * 1000;

    @Override
    public int minConsensusNodes() {
        return ConsensusTypeEnum.RAFT.getMinimalNodeSize();
    }

    @Override
    public Properties getCustomProperties(ParticipantContext context) {
        return new Properties();
    }

    @Override
    public Property[] createActiveProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties) {
        List<Property> properties = new ArrayList<>();

        properties.add(new Property(keyOfNode("system.server.%d.network.host", activeID), address.getHost()));
        properties.add(new Property(keyOfNode("system.server.%d.network.port", activeID), String.valueOf(address.getPort())));
        properties.add(new Property(keyOfNode("system.server.%d.network.secure", activeID), String.valueOf(address.isSecure())));
        properties.add(new Property(keyOfNode("system.server.%d.pubkey", activeID), activePubKey.toBase58()));
        properties.add(new Property("participant.op", "active"));
        properties.add(new Property("active.participant.id", String.valueOf(activeID)));

        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public Property[] createUpdateProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties) {
        return createActiveProperties(address, activePubKey, activeID, customProperties);
    }

    @Override
    public Property[] createDeactiveProperties(PubKey deActivePubKey, int deActiveID, Properties customProperties) {
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("participant.op", "deactive"));
        properties.add(new Property("deactive.participant.id", String.valueOf(deActiveID)));
        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public TransactionResponse submitNodeStateChangeTx(ParticipantContext context, int activeID, TransactionRequest txRequest, List<NodeSettings> origConsensusNodes) {

        if (origConsensusNodes.isEmpty()) {
            throw new IllegalStateException("current consensus node list is empty");
        }

        createRpcClient(context);

        SubmitTxRequest submitTxRequest = new SubmitTxRequest();
        submitTxRequest.setTx(BinaryProtocol.encode(txRequest, TransactionRequest.class));

        RpcResponse rpcResponse = shuffleInvoke(context, origConsensusNodes, submitTxRequest);
        LOGGER.info("submit node state change tx response: {}", rpcResponse);

        TxResponseMessage responseMessage;
        if (!rpcResponse.isSuccess()) {
            responseMessage = new TxResponseMessage();
            responseMessage.setExecutionState(TransactionState.TIMEOUT);
        } else {
            responseMessage = new TxResponseMessage(BinaryProtocol.decode(rpcResponse.getResult()), null);
        }

        return responseMessage;
    }

    @Override
    public boolean startServerBeforeApplyNodeChange() {
        return true;
    }

    @Override
    public WebResponse applyConsensusGroupNodeChange(ParticipantContext context,
                                                     ParticipantNode node,
                                                     @Nullable NetworkAddress changeConsensusNodeAddress,
                                                     List<NodeSettings> origConsensusNodes,
                                                     ManagementController.ParticipantUpdateType type) {
        if (origConsensusNodes.isEmpty()) {
            throw new IllegalStateException("current consensus node list is empty");
        }

        try {
            //等待raft节点服务完全启动
            if (changeConsensusNodeAddress != null) {
                boolean nodeStarted = waitConsensusNodeStarted(context, changeConsensusNodeAddress);
                if (!nodeStarted) {
                    /*
                     * 共识节点启动异常后， 需要先解决异常问题， 然后重启节点。重启之后步骤如下
                     * a. 调用deactive命令删除该共识节点
                     * b. 停止该节点， 拷贝最新账本数据
                     * c. 重启该节点
                     * d. 执行active命令激活节点
                     * e. 执行更新等命令
                     * */
                    return WebResponse.createFailureResult(-1, "raft node may be start failed, check and restart it");
                }
            }

            RaftNodeSettings origNodeSettings = findOrigNodeSetting(node, origConsensusNodes);
            Object request = buildNodeRequest(origNodeSettings, changeConsensusNodeAddress, type);
            if (request == null) {
                throw new IllegalStateException("unsupported operate type " + type.name());
            }

            RpcResponse rpcResponse = shuffleInvoke(context, origConsensusNodes, request);
            LOGGER.info("apply consensus group change response: {}", rpcResponse);

            if (!rpcResponse.isSuccess()) {
                return WebResponse.createFailureResult(-1, rpcResponse.getErrorMessage());
            }

            return WebResponse.createSuccessResult(null);
        } finally {
            shutdownClient(context);
        }

    }


    private Object buildNodeRequest(RaftNodeSettings origNodeSettings, NetworkAddress changeNetworkAddress, ManagementController.ParticipantUpdateType type) {

        switch (type) {
            case ACTIVE:
                if (changeNetworkAddress == null) {
                    throw new IllegalStateException("active node network is empty");
                }
                ParticipantNodeAddRequest addRequest = new ParticipantNodeAddRequest();
                addRequest.setHost(changeNetworkAddress.getHost());
                addRequest.setPort(changeNetworkAddress.getPort());
                return addRequest;
            case UPDATE:
                if (origNodeSettings == null || changeNetworkAddress == null) {
                    throw new IllegalStateException("update node not found in settings or node network is empty");
                }

                ParticipantNodeTransferRequest transferRequest = new ParticipantNodeTransferRequest();
                transferRequest.setPreHost(origNodeSettings.getNetworkAddress().getHost());
                transferRequest.setPrePort(origNodeSettings.getNetworkAddress().getPort());
                transferRequest.setNewHost(changeNetworkAddress.getHost());
                transferRequest.setNewPort(changeNetworkAddress.getPort());

                return transferRequest;
            case DEACTIVE:
                if (origNodeSettings == null) {
                    throw new IllegalStateException("deActive node not found in settings");
                }

                ParticipantNodeRemoveRequest removeRequest = new ParticipantNodeRemoveRequest();
                removeRequest.setHost(origNodeSettings.getNetworkAddress().getHost());
                removeRequest.setPort(origNodeSettings.getNetworkAddress().getPort());
                return removeRequest;
            default:
                return null;
        }
    }

    private RaftNodeSettings findOrigNodeSetting(ParticipantNode node, List<NodeSettings> origConsensusNodes) {
        for (NodeSettings nodeSettings : origConsensusNodes) {
            RaftNodeSettings raftNodeSettings = (RaftNodeSettings) nodeSettings;
            if (raftNodeSettings.getPubKey().equals(node.getPubKey())) {
                return raftNodeSettings;
            }
        }

        return null;
    }

    private void shutdownClient(ParticipantContext context) {
        CliClientServiceImpl clientService = (CliClientServiceImpl) context.getProperty(RPC_CLIENT);
        if (clientService != null) {
            clientService.shutdown();
        }
    }

    private RpcResponse shuffleInvoke(ParticipantContext context, List<NodeSettings> origConsensusNodes, Object request) {
        Collections.shuffle(origConsensusNodes);

        for (NodeSettings nodeSettings : origConsensusNodes) {
            RaftNodeSettings raftNodeSettings = (RaftNodeSettings) nodeSettings;
            Endpoint remoteEndpoint = new Endpoint(raftNodeSettings.getNetworkAddress().getHost(), raftNodeSettings.getNetworkAddress().getPort());
            CliClientServiceImpl cliClientService = createRpcClient(context);

            if (cliClientService.connect(remoteEndpoint)) {
                return invoke(context, remoteEndpoint, request);
            }
        }

        return RpcResponse.fail(-1, "not found connected nodes");
    }

    private RpcResponse invoke(ParticipantContext context, Endpoint endpoint, Object request) {
        RpcResponse response = null;
        try {
            RpcClient client = createRpcClient(context).getRpcClient();
            int timeoutMs = (int) context.getProperty(RPC_QUEST_TIMEOUT_MS);
            response = (RpcResponse) client.invokeSync(endpoint, request, timeoutMs);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            response = RpcResponse.fail(-1, e.getMessage());
        }
        return response;
    }


    private CliClientServiceImpl createRpcClient(ParticipantContext context) {

        if (context.getProperty(RPC_CLIENT) != null) {
            return (CliClientServiceImpl) context.getProperty(RPC_CLIENT);
        }

        RaftConsensusSettings consensusSetting = (RaftConsensusSettings) getConsensusSetting(context);
        RaftNetworkSettings networkSettings = consensusSetting.getNetworkSettings();

        SSLSecurity sslSecurity = context.sslSecurity();
        //启用TLS
        if (sslSecurity != null && sslSecurity.getKeyStore() != null) {
            GmSSLProvider.enableGMSupport(sslSecurity.getProtocol());

            setSystemProperty("bolt.client.ssl.enable", "true");
            setSystemProperty("bolt.client.ssl.keystore", sslSecurity.getTrustStore());
            setSystemProperty("bolt.client.ssl.keystore.password", sslSecurity.getTrustStorePassword());
            setSystemProperty("bolt.client.ssl.keystore.type", sslSecurity.getTrustStoreType() == null ? "JKS" : sslSecurity.getTrustStoreType());
            setSystemProperty("bolt.ssl.protocol", sslSecurity.getProtocol());
            setSystemProperty("bolt.server.ssl.enable", "true");
            setSystemProperty("bolt.server.ssl.keystore", sslSecurity.getKeyStore());
            setSystemProperty("bolt.server.ssl.keyalias", sslSecurity.getKeyAlias());
            setSystemProperty("bolt.server.ssl.keystore.password", sslSecurity.getKeyStorePassword());
            setSystemProperty("bolt.server.ssl.keystore.type", sslSecurity.getKeyStoreType());
            if (sslSecurity.getEnabledProtocols() != null && sslSecurity.getEnabledProtocols().length > 0) {
                setSystemProperty("bolt.ssl.enabled-protocols", String.join(",", sslSecurity.getEnabledProtocols()));
            }
            if (sslSecurity.getCiphers() != null && sslSecurity.getCiphers().length > 0) {
                setSystemProperty("bolt.ssl.ciphers", String.join(",", sslSecurity.getCiphers()));
            }
        }

        CliOptions cliOptions = new CliOptions();
        cliOptions.setRpcConnectTimeoutMs(networkSettings.getRpcConnectTimeoutMs());
        cliOptions.setRpcDefaultTimeout(networkSettings.getRpcDefaultTimeoutMs());
        cliOptions.setRpcInstallSnapshotTimeout(networkSettings.getRpcSnapshotTimeoutMs());
        cliOptions.setTimeoutMs(networkSettings.getRpcRequestTimeoutMs() * 2);
        cliOptions.setMaxRetry(MAX_RETRY_TIMES);

        CliClientServiceImpl clientService = new CliClientServiceImpl();
        clientService.init(cliOptions);

        context.setProperty(RPC_CLIENT, clientService);
        context.setProperty(RPC_QUEST_TIMEOUT_MS, networkSettings.getRpcRequestTimeoutMs() * 2);

        return clientService;
    }

    private boolean waitConsensusNodeStarted(ParticipantContext context, NetworkAddress changeConsensusNodeAddress) {
        CliClientServiceImpl rpcClient = createRpcClient(context);
        Endpoint changeNode = new Endpoint(changeConsensusNodeAddress.getHost(), changeConsensusNodeAddress.getPort());
        int i = 0;
        while (i <= MAX_RETRY_TIMES) {
            i++;
            try {
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (rpcClient.connect(changeNode)) {
                return true;
            }
        }

        return false;
    }

    private void setSystemProperty(String key, String value) {
        if (!StringUtils.isEmpty(value)) {
            System.getProperties().setProperty(key, value);
        }
    }

}
