/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/18 下午2:50
 * Description:
 */
package com.jd.blockchain.consensus.mq;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.SettingsFactory;
import com.jd.blockchain.consensus.client.ClientFactory;
import com.jd.blockchain.consensus.manage.ManageClientFactory;
import com.jd.blockchain.consensus.mq.client.MQClientFactory;
import com.jd.blockchain.consensus.mq.config.MQSettingsFactory;
import com.jd.blockchain.consensus.mq.server.MQNodeServerFactory;
import com.jd.blockchain.consensus.service.NodeServerFactory;
import com.jd.blockchain.ledger.ConsensusTypeEnum;

/**
 * @author shaozhuguang
 * @create 2018/12/18
 * @since 1.0.0
 */

public class MsgQueueConsensusProvider implements ConsensusProvider {

    public static final String NAME = MsgQueueConsensusProvider.class.getName();

    private static MQSettingsFactory settingsFactory = new MQSettingsFactory();

    private static MQClientFactory clientFactory = new MQClientFactory();

    private static MQNodeServerFactory nodeServerFactory = new MQNodeServerFactory();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SettingsFactory getSettingsFactory() {
        return settingsFactory;
    }

    @Override
    public ConsensusTypeEnum getConsensusType() {
        return ConsensusTypeEnum.MQ;
    }

    @Override
    public ClientFactory getClientFactory() {
        return clientFactory;
    }

    @Override
    public NodeServerFactory getServerFactory() {
        return nodeServerFactory;
    }

    @Override
    public ManageClientFactory getManagerClientFactory() {
        // TODO Auto-generated method stub
        throw new IllegalStateException("Not implemented!");
    }
}