package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.kvdb.protocol.proto.DatabaseBaseInfos;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBDatabaseBaseInfo;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBDatabaseBaseInfos;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.kvdb.server.config.DBInfo;
import com.jd.blockchain.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ShowDatabasesExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowDatabasesExecutor.class);

    @Override
    public Message execute(Request request) {
        try {
            LOGGER.debug("execute show databases");
            Collection<DBInfo> dbs = request.getServerContext().getConfig().getDbList().getDatabases();
            KVDBDatabaseBaseInfo[] infos = new KVDBDatabaseBaseInfo[dbs.size()];
            int i = 0;
            for (DBInfo db : dbs) {
                infos[i] = new KVDBDatabaseBaseInfo(db.getName(), db.getDbRootdir(), db.getPartitions(), db.isEnable());
                i++;
            }
            return KVDBMessage.success(request.getId(), new Bytes(BinaryProtocol.encode(new KVDBDatabaseBaseInfos(infos), DatabaseBaseInfos.class)));
        } catch (Exception e) {
            LOGGER.error("execute show databases", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}
