/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.config.MsgQueueConsensusSettings
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/13 下午4:37
 * Description:
 */
package com.jd.blockchain.consensus.mq.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consts.DataCodes;

import utils.Property;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */
@DataContract(code = DataCodes.CONSENSUS_MQ_SETTINGS)
public interface MQConsensusSettings extends ConsensusViewSettings {

    @DataField(order = 0, refContract = true)
    MQNetworkSettings getNetworkSettings();

    @DataField(order = 1, refContract = true)
    MQBlockSettings getBlockSettings();

    @DataField(order = 3, primitiveType = PrimitiveType.BYTES, list=true)
    Property[] getSystemConfigs();
}