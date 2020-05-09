package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.protocol.proto.DatabaseClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.StringUtils;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 切换数据库实例
 */
public class UseExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UseExecutor.class);

    /**
     * @param request
     * @return 返回实例单实例或集群信息
     */
    @Override
    public Message execute(Request request) {
        try {
            String db = BytesUtils.toString(request.getCommand().getParameters()[0].toBytes());
            LOGGER.debug("execute use, db:{}", db);
            if (StringUtils.isEmpty(db)) {
                return KVDBMessage.error(request.getId(), "db name empty");
            } else {
                KVDBInstance kvdbInstance = request.getServerContext().getDatabase(db);
                if (null != kvdbInstance) {
                    request.getSession().setDB(db, kvdbInstance);
                    return KVDBMessage.success(request.getId(),
                            new Bytes(BinaryProtocol.encode(request.getServerContext().getDatabaseInfo(db), DatabaseClusterInfo.class)));
                } else {
                    return KVDBMessage.error(request.getId(), "database not exists");
                }
            }
        } catch (Exception e) {
            LOGGER.error("execute use error", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}
