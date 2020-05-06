package com.jd.blockchain.kvdb.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class KVDBInitializerHandler extends ChannelInitializer<SocketChannel> {

    private final KVDBHandler impl;

    public KVDBInitializerHandler(KVDBHandler impl) {
        this.impl = impl;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        impl.channel(channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        impl.disconnected(ctx);
    }
}
