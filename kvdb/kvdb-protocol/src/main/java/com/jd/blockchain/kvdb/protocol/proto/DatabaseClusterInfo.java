package com.jd.blockchain.kvdb.protocol.proto;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.kvdb.protocol.Constants;

/**
 * 数据库实例信息
 */
@DataContract(code = Constants.DATABASE_INFO)
public interface DatabaseClusterInfo {

    @DataField(order = 0, primitiveType = PrimitiveType.BOOLEAN)
    boolean isClusterMode();

    @DataField(order = 1, refContract = true)
    ClusterItem getClusterItem();

}
