/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.config.MsgQueueConsensusConfig
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 上午11:26
 * Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueBlockSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings;
import utils.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置消息队列的信息
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MsgQueueConsensusConfig implements MsgQueueConsensusSettings {

    private List<NodeSettings> nodeSettingsList = new ArrayList<>();

    private MsgQueueNetworkSettings networkSettings;

    private MsgQueueBlockSettings blockSettings;

    public MsgQueueConsensusConfig addNodeSettings(MsgQueueNodeSettings nodeSettings) {
        nodeSettingsList.add(nodeSettings);
        return this;
    }

    @Override
    public NodeSettings[] getNodes() {
        return nodeSettingsList.toArray(new NodeSettings[nodeSettingsList.size()]);
    }

    @Override
    public MsgQueueNetworkSettings getNetworkSettings() {
        return networkSettings;
    }

    public MsgQueueConsensusConfig setNetworkSettings(MsgQueueNetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
        return this;
    }

    @Override
    public MsgQueueBlockSettings getBlockSettings() {
        return blockSettings;
    }

    public MsgQueueConsensusConfig setBlockSettings(MsgQueueBlockSettings blockSettings) {
        this.blockSettings = blockSettings;
        return this;
    }

    @Override
    public Property[] getSystemConfigs() {
        MsgQueueNetworkSettings networkSettings = getNetworkSettings();
        MsgQueueBlockSettings blockSettings = getBlockSettings();
        Property[] properties = new Property[8];
        properties[0] = new Property("system.msg.queue.server", networkSettings.getServer());
        properties[1] = new Property("system.msg.queue.topic.tx", networkSettings.getTxTopic());
        properties[2] = new Property("system.msg.queue.topic.tx-result", networkSettings.getTxResultTopic());
        properties[3] = new Property("system.msg.queue.topic.msg", networkSettings.getMsgTopic());
        properties[4] = new Property("system.msg.queue.topic.msg-result", networkSettings.getMsgResultTopic());
        properties[5] = new Property("system.msg.queue.topic.block", networkSettings.getBlockTopic());
        properties[6] = new Property("system.msg.queue.block.txsize", String.valueOf(blockSettings.getTxSizePerBlock()));
        properties[7] = new Property("system.msg.queue.block.maxdelay", String.valueOf(blockSettings.getMaxDelayMilliSecondsPerBlock()));
        return properties;
    }
}