/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.config.MsgQueueNetworkConfig
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午4:55
 * Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.mq.settings.MsgQueueNetworkSettings;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MsgQueueNetworkConfig implements MsgQueueNetworkSettings {

    private String server;

    private String txTopic;

    private String txResultTopic;

    private String msgTopic;

    private String blockTopic;

    public MsgQueueNetworkConfig setServer(String server) {
        this.server = server;
        return this;
    }

    public MsgQueueNetworkConfig setTxTopic(String txTopic) {
        this.txTopic = txTopic;
        return this;
    }

    public MsgQueueNetworkConfig setTxResultTopic(String txResultTopic) {
        this.txResultTopic = txResultTopic;
        return this;
    }

    public MsgQueueNetworkConfig setMsgTopic(String msgTopic) {
        this.msgTopic = msgTopic;
        return this;
    }

    public MsgQueueNetworkConfig setBlockTopic(String blockTopic) {
        this.blockTopic = blockTopic;
        return this;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public String getTxTopic() {
        return txTopic;
    }

    @Override
    public String getTxResultTopic() {
        return txResultTopic;
    }

    @Override
    public String getMsgTopic() {
        return msgTopic;
    }

    @Override
    public String getBlockTopic() {
        return blockTopic;
    }
}