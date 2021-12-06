package com.jd.blockchain.peer.service;

import bftsmart.reconfiguration.Reconfiguration;
import bftsmart.reconfiguration.ReconfigureReply;
import bftsmart.reconfiguration.util.HostsConfig;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.reconfiguration.views.MemoryBasedViewStorage;
import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.ServiceProxy;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.peer.web.ManagementController;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.httpservice.utils.web.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import utils.PropertiesUtils;
import utils.Property;
import utils.net.NetworkAddress;
import utils.net.SSLSecurity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.*;

@Component(ConsensusServiceFactory.BFTSMART_PROVIDER)
public class ParticipantManagerService4Bft implements IParticipantManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantManagerService4Bft.class);

    public static final int BFT_CONSENSUS_MIN_NODES = 4;

    @Override
    public int minConsensusNodes() {
        return BFT_CONSENSUS_MIN_NODES;
    }

    @Override
    public Properties getCustomProperties(ParticipantContext context) {
        ConsensusViewSettings csSettings = getConsensusSetting(context);
        return PropertiesUtils.createProperties(((BftsmartConsensusViewSettings) csSettings).getSystemConfigs());
    }

    @Override
    public Property[] createActiveProperties(NetworkAddress address, PubKey activePubKey,
                                             int activeID, Properties systemConfig) {
        int oldServerNum = Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY));
        int oldFNum = Integer.parseInt(systemConfig.getProperty(F_NUM_KEY));
        String oldView = systemConfig.getProperty(SERVER_VIEW_KEY);

        List<Property> properties = new ArrayList<Property>();

        properties.add(new Property(keyOfNode(CONSENSUS_HOST_PATTERN, activeID), address.getHost()));
        properties.add(new Property(keyOfNode(CONSENSUS_PORT_PATTERN, activeID), String.valueOf(address.getPort())));
        properties.add(new Property(keyOfNode(CONSENSUS_SECURE_PATTERN, activeID), String.valueOf(address.isSecure())));
        properties.add(new Property(keyOfNode(PUBKEY_PATTERN, activeID), activePubKey.toBase58()));
        properties.add(new Property(SERVER_NUM_KEY,
                String.valueOf(Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY)) + 1)));
        properties.add(new Property(PARTICIPANT_OP_KEY, "active"));
        properties.add(new Property(ACTIVE_PARTICIPANT_ID_KEY, String.valueOf(activeID)));

        if ((oldServerNum + 1) >= (3 * (oldFNum + 1) + 1)) {
            properties.add(new Property(F_NUM_KEY, String.valueOf(oldFNum + 1)));
        }
        properties.add(new Property(SERVER_VIEW_KEY, createActiveView(oldView, activeID)));

        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public Property[] createUpdateProperties(NetworkAddress address, PubKey activePubKey,
                                             int activeID, Properties systemConfig) {
        String oldView = systemConfig.getProperty(SERVER_VIEW_KEY);

        List<Property> properties = new ArrayList<Property>();

        properties.add(new Property(keyOfNode(CONSENSUS_HOST_PATTERN, activeID), address.getHost()));
        properties.add(new Property(keyOfNode(CONSENSUS_PORT_PATTERN, activeID), String.valueOf(address.getPort())));
        properties.add(new Property(keyOfNode(CONSENSUS_SECURE_PATTERN, activeID), String.valueOf(address.isSecure())));
        properties.add(new Property(keyOfNode(PUBKEY_PATTERN, activeID), activePubKey.toBase58()));
        properties.add(new Property(PARTICIPANT_OP_KEY, "active"));
        properties.add(new Property(ACTIVE_PARTICIPANT_ID_KEY, String.valueOf(activeID)));

        properties.add(new Property(SERVER_VIEW_KEY, createActiveView(oldView, activeID)));

        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public Property[] createDeactiveProperties(PubKey deActivePubKey, int deActiveID, Properties systemConfig) {
        int oldServerNum = Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY));
        int oldFNum = Integer.parseInt(systemConfig.getProperty(F_NUM_KEY));
        String oldView = systemConfig.getProperty(SERVER_VIEW_KEY);

        List<Property> properties = new ArrayList<Property>();

        properties.add(new Property(SERVER_NUM_KEY,
                String.valueOf(Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY)) - 1)));

        if ((oldServerNum - 1) < (3 * oldFNum + 1)) {
            properties.add(new Property(F_NUM_KEY, String.valueOf(oldFNum - 1)));
        }
        properties.add(new Property(SERVER_VIEW_KEY, createDeactiveView(oldView, deActiveID)));

        properties.add(new Property(PARTICIPANT_OP_KEY, "deactive"));

        properties.add(new Property(DEACTIVE_PARTICIPANT_ID_KEY, String.valueOf(deActiveID)));

        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public TransactionResponse submitNodeStateChangeTx(ParticipantContext context, TransactionRequest txRequest, List<NodeSettings> origConsensusNodes) {

        Properties systemConfig = getCustomProperties(context);
        int viewId = ((BftsmartConsensusViewSettings) getConsensusSetting(context)).getViewId();

        TransactionResponse transactionResponse = new TxResponseMessage();
        ServiceProxy peerProxy = createPeerProxy(systemConfig, viewId, origConsensusNodes, context.sslSecurity());
        byte[] result = peerProxy.invokeOrdered(BinaryProtocol.encode(txRequest, TransactionRequest.class));
        if (result == null) {
            ((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.CONSENSUS_NO_REPLY_ERROR);
            return transactionResponse;
        }
        peerProxy.close();
        return txResponseWrapper(BinaryProtocol.decode(result));
    }

    @Override
    public boolean startServerBeforeApplyNodeChange() {
        return false;
    }


    private ServiceProxy createPeerProxy(Properties systemConfig, int viewId, List<NodeSettings> origConsensusNodes, SSLSecurity security) {

        HostsConfig hostsConfig;
        List<HostsConfig.Config> configList = new ArrayList<>();
        List<NodeNetwork> nodeAddresses = new ArrayList<>();

        try {

            int[] origConsensusProcesses = new int[origConsensusNodes.size()];

            for (int i = 0; i < origConsensusNodes.size(); i++) {
                BftsmartNodeSettings node = (BftsmartNodeSettings) origConsensusNodes.get(i);
                origConsensusProcesses[i] = node.getId();
                configList.add(new HostsConfig.Config(node.getId(), node.getNetworkAddress().getHost(),
                        node.getNetworkAddress().getPort(), -1, node.getNetworkAddress().isSecure(), false));
                nodeAddresses.add(
                        new NodeNetwork(node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort(), -1, node.getNetworkAddress().isSecure(), false));
            }

            // 构建共识的代理客户端需要的主机配置和系统参数配置结构
            hostsConfig = new HostsConfig(configList.toArray(new HostsConfig.Config[configList.size()]));

            Properties tempSystemConfig = (Properties) systemConfig.clone();

            // 构建tom 配置
            TOMConfiguration tomConfig = new TOMConfiguration(-(new Random().nextInt(Integer.MAX_VALUE - 2) - 1), tempSystemConfig, hostsConfig);

            View view = new View(viewId, origConsensusProcesses, tomConfig.getF(),
                    nodeAddresses.toArray(new NodeNetwork[nodeAddresses.size()]));

            LOGGER.info("ManagementController start updateView operation!, current view : {}", view.toString());

            // 构建共识的代理客户端，连接目标共识节点，并递交交易进行共识过程
            return new ServiceProxy(tomConfig, new MemoryBasedViewStorage(view), null, null, security);

        } catch (Exception e) {
            throw new CreateProxyClientException("create proxy client exception!");
        }

    }


    private TransactionResponse txResponseWrapper(TransactionResponse txResponse) {
        return new TxResponseMessage(txResponse, null);
    }


    private String createActiveView(String oldView, int id) {

        StringBuilder views = new StringBuilder(oldView);

        views.append(",");

        views.append(id);

        return views.toString();
    }

    private String createDeactiveView(String oldView, int id) {

        StringBuilder newViews = new StringBuilder("");

        String[] viewIdArray = oldView.split(",");

        for (String viewId : viewIdArray) {
            if (Integer.parseInt(viewId) != id) {
                newViews.append(viewId);
                newViews.append(",");
            }
        }
        String newView = newViews.toString();

        return newView.substring(0, newView.length() - 1);
    }

    @Override
    public WebResponse applyConsensusGroupNodeChange(ParticipantContext context,
                                                     ParticipantNode node,
                                                     @Nullable NetworkAddress changeNetworkAddress,
                                                     List<NodeSettings> origConsensusNodes,
                                                     ManagementController.ParticipantUpdateType type) {

        Properties systemConfig = getCustomProperties(context);
        int viewId = ((BftsmartConsensusViewSettings) getConsensusSetting(context)).getViewId();
        SSLSecurity security = context.sslSecurity();

        try {
            View newView = updateView(node, changeNetworkAddress, security, type, systemConfig, viewId, origConsensusNodes);
            if(newView == null){
                throw new IllegalStateException("client recv response timeout, consensus may be stalemate, please restart all nodes!");
            }

            boolean isSuccess = false;
            if(type == ManagementController.ParticipantUpdateType.DEACTIVE){
                if(!newView.isMember(node.getId())){
                    isSuccess = true;
                }
            }else {
                if(newView.isMember(node.getId())){
                    isSuccess = true;
                }
            }

            if (isSuccess) {
                LOGGER.info("update view success!");
            }

        } catch (Exception e) {
            LOGGER.error("updateView exception!", e);
            return WebResponse.createFailureResult(-1,
                    "commit tx to orig consensus, tx execute succ but view update failed, please restart all nodes and copy database for new participant node!");
        }

        return WebResponse.createSuccessResult(null);
    }

    // 通知原有的共识网络更新共识的视图ID
    private View updateView(ParticipantNode node, NetworkAddress networkAddress, SSLSecurity security,
                            ManagementController.ParticipantUpdateType participantUpdateType, Properties systemConfig, int viewId, List<NodeSettings> origConsensusNodes) {

        LOGGER.info("ManagementController start updateView operation!");

        try {

            ServiceProxy peerProxy = createPeerProxy(systemConfig, viewId, origConsensusNodes, security);

            Reconfiguration reconfiguration = new Reconfiguration(peerProxy.getProcessId(), peerProxy);

            if (participantUpdateType == ManagementController.ParticipantUpdateType.ACTIVE) {
                // addServer的第一个参数指待加入共识的新参与方的编号
                reconfiguration.addServer(node.getId(), networkAddress);
            } else if (participantUpdateType == ManagementController.ParticipantUpdateType.DEACTIVE) {
                // 参数为待移除共识节点的id
                reconfiguration.removeServer(node.getId());
            } else if (participantUpdateType == ManagementController.ParticipantUpdateType.UPDATE) {
                // 共识参数修改，先移除后添加
                reconfiguration.removeServer(node.getId());
                reconfiguration.addServer(node.getId(), networkAddress);
            } else {
                throw new IllegalArgumentException("op type error!");
            }

            // 把交易作为reconfig操作的扩展信息携带，目的是为了让该操作上链，便于后续跟踪；
            reconfiguration.addExtendInfo(BinaryProtocol.encode(prepareReconfigTx(), TransactionRequest.class));

            // 执行更新目标共识网络的视图ID
            ReconfigureReply reconfigureReply = reconfiguration.execute();

            peerProxy.close();

            // 返回新视图
            return reconfigureReply.getView();

        } catch (Exception e) {
            throw new ViewUpdateException("view update fail exception!", e);
        }
    }

    // 在指定的账本上准备一笔reconfig操作交易
    private TransactionRequest prepareReconfigTx() {

        ParticipantContext context = ParticipantContext.context();

        HashDigest ledgerHash = context.ledgerHash();
        TxBuilder txbuilder = new TxBuilder(ledgerHash, (Short) context.getProperty(ParticipantContext.HASH_ALG_PROP));

        // This transaction contains one reconfig op
        txbuilder.reconfigs().record();

        TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

        reqBuilder.signAsEndpoint((AsymmetricKeypair) context.getProperty(ParticipantContext.ENDPOINT_SIGNER_PROP));

        return reqBuilder.buildRequest();

    }
}
