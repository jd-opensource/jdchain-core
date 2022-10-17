/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/13 下午4:50
 * Description:
 */
package com.jd.blockchain.consensus.mq.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consts.DataCodes;

/**
 * @author shaozhuguang
 * @create 2018/12/13
 * @since 1.0.0
 */

@DataContract(code = DataCodes.CONSENSUS_MQ_NODE_SETTINGS)
public interface MQNodeSettings extends NodeSettings {

    @DataField(order = 0, primitiveType = PrimitiveType.INT32)
    int getId();

    /**
     * 服务地址
     *
     * @return
     */
    @DataField(order = 2, primitiveType = PrimitiveType.TEXT)
    String getHost();
}