package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnableDatabaseExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnableDatabaseExecutor.class);

    @Override
    public Message execute(Request request) {
        try {

            String database = BytesUtils.toString(request.getCommand().getParameters()[0].toBytes());
            request.getServerContext().enableDatabase(database);

            return KVDBMessage.success(request.getId());
        } catch (Exception e) {
            LOGGER.error("execute create databases", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}
