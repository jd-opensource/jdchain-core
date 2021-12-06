package com.jd.blockchain.peer.service;

import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.common.base.Strings;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.raft.rpc.*;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNetworkSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.peer.web.ManagementController;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.httpservice.utils.web.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import utils.Property;
import utils.net.NetworkAddress;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component(ConsensusServiceFactory.RAFT_PROVIDER)
public class ParticipantManagerService4Raft implements IParticipantManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantManagerService4Raft.class);

    private static final int RAFT_CONSENSUS_MIN_NODES = 1;
    private static final String RAFT_PATH_KEY = "raft_path";
    private static final int MAX_RETRY_TIMES = 3;
    private static final String RPC_QUEST_TIMEOUT_MS = "rpc_quest_timeout_ms";
    private static final String RPC_CLIENT = "rpc_client";

    @Override
    public int minConsensusNodes() {
        return RAFT_CONSENSUS_MIN_NODES;
    }

    @Override
    public Properties getCustomProperties(ParticipantContext context) {
        Properties properties = new Properties();
        properties.setProperty(RAFT_PATH_KEY, String.valueOf(context.getProperty(IParticipantManagerService.RAFT_CONSENSUS_NODE_STORAGE)));
        return properties;
    }

    @Override
    public Property[] createActiveProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties) {

        String raftPath = customProperties.getProperty(RAFT_PATH_KEY);
        if (Strings.isNullOrEmpty(raftPath)) {
            throw new IllegalStateException("raft path is missing");
        }

        List<Property> properties = new ArrayList<>();

        properties.add(new Property(keyOfNode("system.server.%d.network.host", activeID), address.getHost()));
        properties.add(new Property(keyOfNode("system.server.%d.network.port", activeID), String.valueOf(address.getPort())));
        properties.add(new Property(keyOfNode("system.server.%d.network.secure", activeID), String.valueOf(address.isSecure())));
        properties.add(new Property(keyOfNode("system.server.%d.raft.path", activeID), raftPath));
        properties.add(new Property(keyOfNode("system.server.%s.pubkey", activeID), activePubKey.toBase58()));
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
    public TransactionResponse submitNodeStateChangeTx(ParticipantContext context, TransactionRequest txRequest, List<NodeSettings> origConsensusNodes) {

        if (origConsensusNodes.isEmpty()) {
            throw new IllegalStateException("current consensus node list is empty");
        }

        createRpcClient(context);

        SubmitTxRequest submitTxRequest = new SubmitTxRequest();
        submitTxRequest.setTx(BinaryProtocol.encode(txRequest, TransactionRequest.class));

        //todo choose nodes
        RaftNodeSettings nodeSettings = (RaftNodeSettings) origConsensusNodes.get(0);
        Endpoint remoteEndpoint = new Endpoint(nodeSettings.getNetworkAddress().getHost(), nodeSettings.getNetworkAddress().getPort());

        RpcResponse rpcResponse = invoke(context, remoteEndpoint, submitTxRequest);

        TxResponseMessage responseMessage = null;
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
                                                     @Nullable NetworkAddress changeNetworkAddress,
                                                     List<NodeSettings> origConsensusNodes,
                                                     ManagementController.ParticipantUpdateType type) {
        if (origConsensusNodes.isEmpty()) {
            throw new IllegalStateException("current consensus node list is empty");
        }

        try {

            RaftNodeSettings origNodeSettings = findOrigNodeSetting(node, origConsensusNodes);
            Object request = buildNodeRequest(origNodeSettings, changeNetworkAddress, type);
            if (request == null) {
                throw new IllegalStateException("unsupported operate type " + type.name());
            }

            RaftNodeSettings nodeSettings = (RaftNodeSettings) origConsensusNodes.get(0);
            Endpoint remoteEndpoint = new Endpoint(nodeSettings.getNetworkAddress().getHost(), nodeSettings.getNetworkAddress().getPort());

            RpcResponse rpcResponse = invoke(context, remoteEndpoint, request);
            if (!rpcResponse.isSuccess()) {
                return WebResponse.createFailureResult(-1, rpcResponse.getErrorMessage());
            }

            return WebResponse.createSuccessResult(null);
        } finally {
            shutdownClient(context);
        }

    }

    private Object buildNodeRequest(RaftNodeSettings origNodeSettings, NetworkAddress changeNetworkAddress, ManagementController.ParticipantUpdateType type) {

        if (type == ManagementController.ParticipantUpdateType.ACTIVE) {

            if (changeNetworkAddress == null) {
                throw new IllegalStateException("active node network is empty");
            }

            ParticipantNodeAddRequest addRequest = new ParticipantNodeAddRequest();
            addRequest.setHost(changeNetworkAddress.getHost());
            addRequest.setPort(changeNetworkAddress.getPort());
            return addRequest;
        }

        if (type == ManagementController.ParticipantUpdateType.DEACTIVE) {

            if (origNodeSettings == null) {
                throw new IllegalStateException("deActive node not found in settings");
            }

            ParticipantNodeRemoveRequest removeRequest = new ParticipantNodeRemoveRequest();
            removeRequest.setHost(origNodeSettings.getNetworkAddress().getHost());
            removeRequest.setPort(origNodeSettings.getNetworkAddress().getPort());
            return removeRequest;
        }

        if (type == ManagementController.ParticipantUpdateType.UPDATE) {

            if (origNodeSettings == null || changeNetworkAddress == null) {
                throw new IllegalStateException("update node not found in settings or node network is empty");
            }

            ParticipantNodeTransferRequest transferRequest = new ParticipantNodeTransferRequest();
            transferRequest.setPreHost(origNodeSettings.getNetworkAddress().getHost());
            transferRequest.setPrePort(origNodeSettings.getNetworkAddress().getPort());
            transferRequest.setNewHost(changeNetworkAddress.getHost());
            transferRequest.setNewPort(changeNetworkAddress.getPort());

            return transferRequest;
        }

        return null;
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

    private RpcResponse invoke(ParticipantContext context, Endpoint endpoint, Object request) {
        RpcResponse response = null;
        try {
            RpcClient client = ((CliClientServiceImpl) context.getProperty(RPC_CLIENT)).getRpcClient();
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


}
