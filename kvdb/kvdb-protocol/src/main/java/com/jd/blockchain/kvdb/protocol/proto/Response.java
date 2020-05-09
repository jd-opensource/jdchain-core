package com.jd.blockchain.kvdb.protocol.proto;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.kvdb.protocol.Constants;
import com.jd.blockchain.utils.Bytes;

/**
 * 返回消息
 */
@DataContract(code = Constants.RESPONSE)
public interface Response extends MessageContent {

    /**
     * @return 状态, {@link Constants#SUCCESS}, {@link Constants#ERROR}
     */
    @DataField(order = 0, primitiveType = PrimitiveType.INT32)
    int getCode();

    /**
     * @return 结果
     */
    @DataField(order = 1, list = true, primitiveType = PrimitiveType.BYTES)
    Bytes[] getResult();

}
