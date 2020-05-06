package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.proto.DatabaseBaseInfo;
import com.jd.blockchain.kvdb.protocol.proto.DatabaseBaseInfos;

public class KVDBDatabaseBaseInfos implements DatabaseBaseInfos {

    private DatabaseBaseInfo[] baseInfos;

    public KVDBDatabaseBaseInfos(DatabaseBaseInfo[] baseInfos) {
        this.baseInfos = baseInfos;
    }

    @Override
    public DatabaseBaseInfo[] getBaseInfos() {
        return baseInfos;
    }

    public void setBaseInfos(DatabaseBaseInfo[] baseInfos) {
        this.baseInfos = baseInfos;
    }
}
