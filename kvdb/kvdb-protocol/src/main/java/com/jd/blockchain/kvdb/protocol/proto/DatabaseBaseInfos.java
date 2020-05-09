package com.jd.blockchain.kvdb.protocol.proto;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.kvdb.protocol.Constants;

/**
 * 数据库实例基础信息列表
 */
@DataContract(code = Constants.DATABASE_BASE_INFO_LIST)
public interface DatabaseBaseInfos {

    @DataField(order = 0, refContract = true, list = true)
    DatabaseBaseInfo[] getBaseInfos();

}
