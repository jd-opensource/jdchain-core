package com.jd.blockchain.kvdb.protocol.proto;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.kvdb.protocol.Constants;

/**
 * KVDB 消息封装
 */
@DataContract(code = Constants.MESSAGE)
public interface Message {

    /**
     * 消息ID
     *
     * @return
     */
    @DataField(order = 0, primitiveType = PrimitiveType.TEXT)
    String getId();

    /**
     * 消息体
     *
     * @return
     */
    @DataField(order = 2, refContract = true, genericContract = true)
    MessageContent getContent();

}
