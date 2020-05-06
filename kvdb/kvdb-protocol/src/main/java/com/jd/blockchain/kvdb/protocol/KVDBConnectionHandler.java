package com.jd.blockchain.kvdb.protocol;

import com.jd.blockchain.kvdb.protocol.proto.Message;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class KVDBConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBConnectionHandler.class);

    private final KVDBHandler impl;

    public KVDBConnectionHandler(KVDBHandler impl) {
        this.impl = impl;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        impl.connected(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            impl.receive(ctx, (Message) msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("channel inactive");
        impl.disconnected(ctx);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.debug("uncaught exception", cause);
        impl.disconnected(ctx);
        ctx.close();
    }
}
