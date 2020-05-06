package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.kvdb.protocol.proto.ClusterInfo;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterInfoExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInfoExecutor.class);

    @Override
    public Message execute(Request request) {
        try {
            LOGGER.debug("execute cluster sync");
            ClusterInfo info = request.getServerContext().getClusterInfo();
            return KVDBMessage.success(request.getId(), new Bytes(BinaryProtocol.encode(info, ClusterInfo.class)));
        } catch (Exception e) {
            LOGGER.error("execute cluster sync", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}
