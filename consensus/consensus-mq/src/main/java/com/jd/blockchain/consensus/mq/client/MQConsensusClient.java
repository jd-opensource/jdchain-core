/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.client.MsgQueueConsensusClient
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午3:23
 * Description:
 */
package com.jd.blockchain.consensus.mq.client;

import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.consensus.mq.MQFactory;
import com.jd.blockchain.consensus.mq.consumer.MQConsumer;
import com.jd.blockchain.consensus.mq.producer.MQProducer;
import com.jd.blockchain.consensus.mq.settings.MQClientSettings;
import com.jd.blockchain.consensus.mq.settings.MQNetworkSettings;

/**
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MQConsensusClient implements ConsensusClient {

    private boolean isConnected;

    private MQMessageTransmitter transmitter;

    private MQNetworkSettings msgQueueNetworkSettings;

    private MQClientSettings clientSettings;

    public MQConsensusClient setClientSettings(MQClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        return this;
    }

    public MQConsensusClient setMsgQueueNetworkSettings(MQNetworkSettings msgQueueNetworkSettings) {
        this.msgQueueNetworkSettings = msgQueueNetworkSettings;
        return this;
    }

    public void init() {
        String server = msgQueueNetworkSettings.getServer();
        String txTopic = msgQueueNetworkSettings.getTxTopic();
        String txResultTopic = msgQueueNetworkSettings.getTxResultTopic();
        String msgTopic = msgQueueNetworkSettings.getMsgTopic();
        String msgResultTopic = msgQueueNetworkSettings.getMsgResultTopic();

        MQProducer txProducer = MQFactory.newTxProducer(server, txTopic);
        MQProducer msgProducer = MQFactory.newMsgProducer(server, msgTopic);
        MQConsumer txResultConsumer = MQFactory.newTxResultConsumer(server, txResultTopic);
        MQConsumer msgResultConsumer = MQFactory.newMsgResultConsumer(server, msgResultTopic);

        transmitter = new MQMessageTransmitter()
                .setTxProducer(txProducer)
                .setMsgProducer(msgProducer)
                .setTxResultConsumer(txResultConsumer)
                .setMsgResultConsumer(msgResultConsumer)
        ;
    }

    @Override
    public MessageService getMessageService() {
        return transmitter;
    }

    @Override
    public ClientSettings getSettings() {
        return clientSettings;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public synchronized void connect() {
        if (!isConnected) {
            try {
                this.transmitter.connect();
                isConnected = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (isConnected) {
            transmitter.close();
            isConnected = false;
        }
    }
}