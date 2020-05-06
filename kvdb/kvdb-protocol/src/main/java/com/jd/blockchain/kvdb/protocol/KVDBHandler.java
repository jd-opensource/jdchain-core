package com.jd.blockchain.kvdb.protocol;

import com.jd.blockchain.kvdb.protocol.proto.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;

public interface KVDBHandler {

    /**
     * 初始化channel, {@link KVDBInitializerHandler#initChannel(SocketChannel)}中调用
     *
     * @param channel
     */
    void channel(SocketChannel channel);

    /**
     * 连接成功后操作，{@link KVDBConnectionHandler#channelActive(ChannelHandlerContext)}中调用
     *
     * @param ctx
     * @throws InterruptedException
     */
    void connected(ChannelHandlerContext ctx) throws InterruptedException;

    /**
     * 断开连接后操作，{@link KVDBConnectionHandler#channelInactive(ChannelHandlerContext)}中调用
     *
     * @param ctx
     * @throws InterruptedException
     */
    void disconnected(ChannelHandlerContext ctx);

    /**
     * 接收到消息时操作，{@link KVDBConnectionHandler#channelRead(ChannelHandlerContext ctx, Object msg)}中调用
     *
     * @param ctx
     * @throws InterruptedException
     */
    void receive(ChannelHandlerContext ctx, Message message);

}
