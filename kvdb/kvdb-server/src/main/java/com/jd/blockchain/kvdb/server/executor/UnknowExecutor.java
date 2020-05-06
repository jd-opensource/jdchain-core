package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.server.Request;

/**
 * 处理未被识别的操作
 */
public class UnknowExecutor implements Executor {

    @Override
    public Message execute(Request request) {

        return KVDBMessage.error(request.getId(), "un support command: " + request.getCommand().getName());
    }
}
