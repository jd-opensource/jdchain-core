package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.protocol.proto.Command;

/**
 * 请求
 */
public interface Request {

    // 消息ID
    String getId();

    // 操作命令
    Command getCommand();

    // 当前会话
    Session getSession();

    // 上下文
    ServerContext getServerContext();

}
