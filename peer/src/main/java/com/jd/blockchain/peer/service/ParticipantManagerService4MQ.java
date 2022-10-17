package com.jd.blockchain.peer.service;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.mq.MQFactory;
import com.jd.blockchain.consensus.mq.client.MQMessageTransmitter;
import com.jd.blockchain.consensus.mq.consumer.MQConsumer;
import com.jd.blockchain.consensus.mq.event.PeerActiveMessage;
import com.jd.blockchain.consensus.mq.event.PeerInactiveMessage;
import com.jd.blockchain.consensus.mq.event.binaryproto.ExtendType;
import com.jd.blockchain.consensus.mq.event.binaryproto.UnOrderEvent;
import com.jd.blockchain.consensus.mq.producer.MQProducer;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.peer.web.ManagementController;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.httpservice.utils.web.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import utils.Property;
import utils.concurrent.AsyncFuture;
import utils.io.BytesUtils;
import utils.net.NetworkAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Component(ConsensusTypeEnum.MQ_PROVIDER)
public class ParticipantManagerService4MQ implements IParticipantManagerService {

    private static final int MQ_INVOKE_TIMEOUT = 120000; // milliseconds
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantManagerService4MQ.class);

    @Override
    public int minConsensusNodes() {
        return ConsensusTypeEnum.MQ.getMinimalNodeSize();
    }

    @Override
    public Properties getCustomProperties(ParticipantContext context) {
        return new Properties();
    }

    @Override
    public Property[] createActiveProperties(
            NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties) {
        List<Property> properties = new ArrayList<>();

        properties.add(
                new Property(keyOfNode("system.server.%s.pubkey", activeID), activePubKey.toBase58()));
        properties.add(
                new Property(keyOfNode("system.server.%s.host", activeID), address.getHost()));
        properties.add(new Property("participant.op", "active"));
        properties.add(new Property("participant.id", String.valueOf(activeID)));

        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public Property[] createUpdateProperties(
            NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties) {
        return createActiveProperties(address, activePubKey, activeID, customProperties);
    }

    @Override
    public Property[] createDeactiveProperties(
            PubKey deActivePubKey, int deActiveID, Properties customProperties) {
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("participant.op", "deactive"));
        properties.add(new Property("participant.id", String.valueOf(deActiveID)));
        properties.add(
                new Property(keyOfNode("system.server.%s.pubkey", deActiveID), deActivePubKey.toBase58()));
        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public TransactionResponse submitNodeStateChangeTx(
            ParticipantContext context,
            ParticipantNode node,
            TransactionRequest txRequest,
            List<NodeSettings> origConsensusNodes,
            ManagementController.ParticipantUpdateType updateType) {
        TxResponseMessage responseMessage = new TxResponseMessage();
        MQMessageTransmitter mqClient = null;
        try {
            boolean updateViewSuccess = true;
            mqClient = createMQClient(context);
            // 更新操作不用发送 UnOrder 消息
            if (!updateType.equals(ManagementController.ParticipantUpdateType.UPDATE)) {
                // 发送拓扑变更消息
                UnOrderEvent unOrderEvent =
                        updateType.equals(ManagementController.ParticipantUpdateType.DEACTIVE)
                                ? new PeerInactiveMessage(ExtendType.PEER_INACTIVE, node.getAddress().toBase58())
                                : new PeerActiveMessage(ExtendType.PEER_ACTIVE, node.getAddress().toBase58());
                AsyncFuture<byte[]> msgFuture = mqClient.sendUnordered(BinaryProtocol.encode(unOrderEvent));
                byte[] result = msgFuture.get(MQ_INVOKE_TIMEOUT, TimeUnit.MILLISECONDS);
                if (result == null) {
                    updateViewSuccess = false;
                    responseMessage.setExecutionState(TransactionState.TIMEOUT);
                } else if (!BytesUtils.toBoolean(result[0])) {
                    updateViewSuccess = false;
                    responseMessage.setExecutionState(TransactionState.CONSENSUS_ERROR);
                }
            }
            if (updateViewSuccess) {
                // 发送激活交易
                AsyncFuture<byte[]> txFuture =
                        mqClient.sendOrdered(BinaryProtocol.encode(txRequest, TransactionRequest.class));
                byte[] result = txFuture.get(MQ_INVOKE_TIMEOUT, TimeUnit.MILLISECONDS);
                if (result == null) {
                    responseMessage.setExecutionState(TransactionState.TIMEOUT);
                } else {
                    responseMessage = new TxResponseMessage(BinaryProtocol.decode(result), null);
                }
            }
        } catch (Exception e) {
            LOGGER.error("message or tx execute error", e);
            responseMessage.setExecutionState(TransactionState.CONSENSUS_ERROR);
        } finally {
            if (null != mqClient) {
                mqClient.close();
            }
        }
        return responseMessage;
    }

    @Override
    public boolean startServerBeforeApplyNodeChange() {
        return true;
    }

    @Override
    public WebResponse applyConsensusGroupNodeChange(
            ParticipantContext context,
            ParticipantNode node,
            NetworkAddress changeNetworkAddress,
            List<NodeSettings> origConsensusNodes,
            ManagementController.ParticipantUpdateType type) {
        return WebResponse.createSuccessResult("");
    }

    private MQMessageTransmitter createMQClient(ParticipantContext context) throws Exception {
        MQConsensusSettings consensusSettings = (MQConsensusSettings) getConsensusSetting(context);
        String server = consensusSettings.getNetworkSettings().getServer();
        String txTopic = consensusSettings.getNetworkSettings().getTxTopic();
        String txResultTopic = consensusSettings.getNetworkSettings().getTxResultTopic();
        String msgTopic = consensusSettings.getNetworkSettings().getMsgTopic();
        String msgResultTopic = consensusSettings.getNetworkSettings().getMsgResultTopic();

        MQProducer txProducer = MQFactory.newTxProducer(server, txTopic);
        MQProducer msgProducer = MQFactory.newMsgProducer(server, msgTopic);
        MQConsumer txResultConsumer = MQFactory.newTxResultConsumer(server, txResultTopic);
        MQConsumer msgResultConsumer = MQFactory.newMsgResultConsumer(server, msgResultTopic);

        MQMessageTransmitter transmitter =
                new MQMessageTransmitter()
                        .setTxProducer(txProducer)
                        .setTxResultConsumer(txResultConsumer)
                        .setMsgProducer(msgProducer)
                        .setMsgResultConsumer(msgResultConsumer);
        transmitter.connect();

        return transmitter;
    }
}