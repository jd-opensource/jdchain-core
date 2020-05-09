package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.server.Request;

/**
 * 命令执行接口
 */
public interface Executor {

    Message execute(Request request);

}
