package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchBeginExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchBeginExecutor.class);

    @Override
    public Message execute(Request request) {

        LOGGER.debug("execute begin batch");

        try {
            request.getSession().batchBegin();
            return KVDBMessage.success(request.getId());
        } catch (Exception e) {
            LOGGER.debug("execute batch begin error", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}
