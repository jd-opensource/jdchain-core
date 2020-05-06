package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.protocol.proto.Command;
import com.jd.blockchain.kvdb.protocol.proto.Message;

/**
 * 请求封装
 */
public class KVDBRequest implements Request {
    // 当前服务器上下文
    private final ServerContext server;
    // 当前连接会话
    private final Session session;
    // 消息
    private final Message message;

    public KVDBRequest(ServerContext server, Session session, Message message) {
        this.server = server;
        this.session = session;
        this.message = message;
    }

    @Override
    public String getId() {
        return message.getId();
    }

    @Override
    public Command getCommand() {
        return (Command) message.getContent();
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public ServerContext getServerContext() {
        return server;
    }

}
