package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisableDatabaseExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableDatabaseExecutor.class);

    @Override
    public Message execute(Request request) {
        try {
            String database = BytesUtils.toString(request.getCommand().getParameters()[0].toBytes());
            request.getServerContext().disableDatabase(database);

            return KVDBMessage.success(request.getId());
        } catch (Exception e) {
            LOGGER.error("execute create databases", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}
