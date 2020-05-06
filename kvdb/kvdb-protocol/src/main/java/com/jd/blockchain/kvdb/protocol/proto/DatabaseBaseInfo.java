package com.jd.blockchain.kvdb.protocol.proto;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.kvdb.protocol.Constants;

/**
 * 数据库实例基础信息
 */
@DataContract(code = Constants.DATABASE_BASE_INFO)
public interface DatabaseBaseInfo {

    /**
     * @return 数据库名称
     */
    @DataField(order = 0, primitiveType = PrimitiveType.TEXT)
    String getName();

    /**
     * @return 数据库根目录
     */
    @DataField(order = 1, primitiveType = PrimitiveType.TEXT)
    String getRootDir();

    /**
     * @return 数据库分片数量
     */
    @DataField(order = 2, primitiveType = PrimitiveType.INT32)
    Integer getPartitions();

    /**
     * @return 数据库分片数量
     */
    @DataField(order = 3, primitiveType = PrimitiveType.BOOLEAN)
    boolean isEnable();

}
