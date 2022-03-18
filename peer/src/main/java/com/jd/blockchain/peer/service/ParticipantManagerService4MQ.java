package com.jd.blockchain.peer.service;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.mq.client.DefaultMessageTransmitter;
import com.jd.blockchain.consensus.mq.consumer.MsgQueueConsumer;
import com.jd.blockchain.consensus.mq.event.ExtendMessage;
import com.jd.blockchain.consensus.mq.event.ExtendMessageResult;
import com.jd.blockchain.consensus.mq.event.MessageConvertor;
import com.jd.blockchain.consensus.mq.event.MessageType;
import com.jd.blockchain.consensus.mq.factory.MsgQueueFactory;
import com.jd.blockchain.consensus.mq.producer.MsgQueueProducer;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
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
import utils.net.NetworkAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Component(ConsensusTypeEnum.MQ_PROVIDER)
public class ParticipantManagerService4MQ implements IParticipantManagerService {

    private static final int MQ_INVOKE_TIMEOUT = 120000;// milliseconds
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
    public Property[] createActiveProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties) {
        List<Property> properties = new ArrayList<>();

        properties.add(new Property(keyOfNode("system.server.%s.pubkey", activeID), activePubKey.toBase58()));
        properties.add(new Property("participant.op", "active"));
        properties.add(new Property("participant.id", String.valueOf(activeID)));

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
        properties.add(new Property("participant.id", String.valueOf(deActiveID)));
        properties.add(new Property(keyOfNode("system.server.%s.pubkey", deActiveID), deActivePubKey.toBase58()));
        return properties.toArray(new Property[properties.size()]);
    }

    @Override
    public TransactionResponse submitNodeStateChangeTx(ParticipantContext context, int activeID, TransactionRequest txRequest, List<NodeSettings> origConsensusNodes) {
        TxResponseMessage responseMessage = new TxResponseMessage();
        DefaultMessageTransmitter mqClient = null;
        try {
            // 初始化区块队列
            initBlockQueue(context, activeID);
            // 发送拓扑变更消息，通知领导者推送落块消息
            mqClient = createMQClient(context);
            ExtendMessage extendMessage = new ExtendMessage(MessageType.RECONFIGURE, null);
            AsyncFuture<byte[]> msgFuture = mqClient.sendUnordered(MessageConvertor.serializeExtendMessage(extendMessage));
            byte[] result = msgFuture.get(MQ_INVOKE_TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null) {
                responseMessage.setExecutionState(TransactionState.TIMEOUT);
            } else {
                ExtendMessageResult messageResult = MessageConvertor.convertBytesExtendMessageResult(result);
                if (!messageResult.isSuccess()) {
                    LOGGER.error("extend message execute error: {}", messageResult.getError());
                    responseMessage.setExecutionState(TransactionState.CONSENSUS_ERROR);
                } else {
                    // 发送激活交易
                    AsyncFuture<byte[]> txFuture = mqClient.sendOrdered(BinaryProtocol.encode(txRequest, TransactionRequest.class));
                    result = txFuture.get(MQ_INVOKE_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (result == null) {
                        responseMessage.setExecutionState(TransactionState.TIMEOUT);
                        return responseMessage;
                    }
                    return BinaryProtocol.decode(result);
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
    public WebResponse applyConsensusGroupNodeChange(ParticipantContext context, ParticipantNode node,
                                                     NetworkAddress changeNetworkAddress,
                                                     List<NodeSettings> origConsensusNodes,
                                                     ManagementController.ParticipantUpdateType type) {
        return WebResponse.createSuccessResult("");
    }

    private DefaultMessageTransmitter createMQClient(ParticipantContext context) throws Exception {
        MsgQueueConsensusSettings consensusSettings = (MsgQueueConsensusSettings) getConsensusSetting(context);
        String server = consensusSettings.getNetworkSettings().getServer();
        String txTopic = consensusSettings.getNetworkSettings().getTxTopic();
        String txResultTopic = consensusSettings.getNetworkSettings().getTxResultTopic();
        String msgTopic = consensusSettings.getNetworkSettings().getMsgTopic();
        String msgResultTopic = consensusSettings.getNetworkSettings().getMsgResultTopic();
        MsgQueueProducer txProducer = MsgQueueFactory.newProducer(server, txTopic, false);
        MsgQueueConsumer txResultConsumer = MsgQueueFactory.newConsumer(server, txResultTopic, false);
        MsgQueueProducer msgProducer = MsgQueueFactory.newProducer(server, msgTopic, false);
        MsgQueueConsumer msgResultConsumer = MsgQueueFactory.newConsumer(server, msgResultTopic, false);

        DefaultMessageTransmitter transmitter = new DefaultMessageTransmitter()
                .setTxProducer(txProducer)
                .setTxResultConsumer(txResultConsumer)
                .setMsgProducer(msgProducer)
                .setMsgResultConsumer(msgResultConsumer);
        transmitter.connect();

        return transmitter;
    }

    /**
     * 初始化区块队列
     *
     * @param context
     * @param nodeId
     * @throws Exception
     */
    private void initBlockQueue(ParticipantContext context, int nodeId) throws Exception {
        MsgQueueConsensusSettings consensusSettings = (MsgQueueConsensusSettings) getConsensusSetting(context);
        String server = consensusSettings.getNetworkSettings().getServer();
        String blockTopic = consensusSettings.getNetworkSettings().getBlockTopic();
        MsgQueueConsumer blockConsumer = MsgQueueFactory.newConsumer(nodeId, server, blockTopic, true);
        blockConsumer.connect(null);
        blockConsumer.start();
        blockConsumer.close();
    }
}
