package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.kvdb.protocol.*;
import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import com.jd.blockchain.kvdb.protocol.client.NettyClient;
import com.jd.blockchain.kvdb.protocol.proto.*;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.executor.*;
import com.jd.blockchain.utils.io.BytesUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.jd.blockchain.kvdb.protocol.proto.Command.CommandType.*;

public class KVDBServer implements KVDBHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBServer.class);

    private static final int CLUSTER_CONFIRM_TIME_OUT = 3000;
    private final KVDBServerContext serverContext;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture future;
    private ChannelFuture managerFuture;

    /**
     * Whether this server is ready to service.
     * After cluster confirmed ready will be set to true.
     */
    private boolean ready = false;

    public KVDBServer(KVDBServerContext serverContext) {
        this.serverContext = serverContext;
        bindExecutors();
    }

    private void bindExecutors() {
        serverContext.addExecutor(USE.getCommand(), new UseExecutor());
        serverContext.addExecutor(SHOW_DATABASES.getCommand(), new ShowDatabasesExecutor());
        serverContext.addExecutor(CREATE_DATABASE.getCommand(), new CreateDatabaseExecutor());
        serverContext.addExecutor(ENABLE_DATABASE.getCommand(), new EnableDatabaseExecutor());
        serverContext.addExecutor(DISABLE_DATABASE.getCommand(), new DisableDatabaseExecutor());
        serverContext.addExecutor(DROP_DATABASE.getCommand(), new DropDatabaseExecutor());
        serverContext.addExecutor(CLUSTER_INFO.getCommand(), new ClusterInfoExecutor());
        serverContext.addExecutor(EXISTS.getCommand(), new ExistsExecutor());
        serverContext.addExecutor(GET.getCommand(), new GetExecutor());
        serverContext.addExecutor(PUT.getCommand(), new PutExecutor());
        serverContext.addExecutor(BATCH_BEGIN.getCommand(), new BatchBeginExecutor());
        serverContext.addExecutor(BATCH_ABORT.getCommand(), new BatchAbortExecutor());
        serverContext.addExecutor(BATCH_COMMIT.getCommand(), new BatchCommitExecutor());
        serverContext.addExecutor(UNKNOWN.getCommand(), new UnknowExecutor());
    }

    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

        ServerBootstrap bootstrap = new ServerBootstrap();
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new KVDBInitializerHandler(this))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        future = bootstrap.bind(serverContext.getConfig().getKvdbConfig().getHost(), serverContext.getConfig().getKvdbConfig().getPort());
        future.syncUninterruptibly();

        managerFuture = bootstrap.bind("127.0.0.1", serverContext.getConfig().getKvdbConfig().getManagerPort());
        managerFuture.syncUninterruptibly();

        LOGGER.info("server started: {}:{}", serverContext.getConfig().getKvdbConfig().getHost(), serverContext.getConfig().getKvdbConfig().getPort());

        // Confirm cluster settings
        clusterConfirm();

        ready = true;
    }

    public void stop() {
        try {
            if (future != null) {
                closeFuture(future.channel().close());
            }
            future = null;
            if (managerFuture != null) {
                closeFuture(managerFuture.channel().close());
            }
            managerFuture = null;
        } finally {
            workerGroup = closeWorker(workerGroup);
            bossGroup = closeWorker(bossGroup);
        }

        serverContext.stop();

        LOGGER.info("server stopped");
    }

    private void closeFuture(Future<?> future) {
        LOGGER.debug("closing future");
        future.syncUninterruptibly();
        LOGGER.debug("future closed");
    }

    private EventLoopGroup closeWorker(EventLoopGroup worker) {
        if (worker != null) {
            closeFuture(worker.shutdownGracefully());
        }
        return null;
    }

    private String sourceKey(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        return remoteAddress.getHostName() + ":" + remoteAddress.getPort();
    }

    public void channel(SocketChannel channel) {
        LOGGER.debug("new channel: {}", sourceKey(channel));

        channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4))
                .addLast("kvdbDecoder", new KVDBDecoder())
                .addLast(new LengthFieldPrepender(4, 0, false))
                .addLast("kvdbEncoder", new KVDBEncoder())
                .addLast(new KVDBConnectionHandler(this));
    }

    public void connected(ChannelHandlerContext ctx) {
        String sourceKey = sourceKey(ctx.channel());
        LOGGER.debug("client connected: {}", sourceKey);
        getSession(ctx, sourceKey);
    }

    private Session getSession(ChannelHandlerContext ctx, String sourceKey) {
        return serverContext.getSession(sourceKey, key -> new KVDBSession(key, ctx));
    }

    public void disconnected(ChannelHandlerContext ctx) {
        String sourceKey = sourceKey(ctx.channel());

        LOGGER.debug("client disconnected: {}", sourceKey);

        serverContext.removeSession(sourceKey);
    }

    public void receive(ChannelHandlerContext ctx, Message message) {
        String sourceKey = sourceKey(ctx.channel());

        LOGGER.debug("message received: {}", sourceKey);

        Command command = (Command) message.getContent();

        // 仅当服务器就绪后才能对外提供服务，集群配置查询命令除外，用于同步确认集群环境
        if (!ready && command.getName().equals(CLUSTER_INFO.getCommand())) {
            serverContext.processCommand(sourceKey, message);
        } else {
            // 解析客户端IP地址，针对非开放操作仅对本机地址通过管理服务端口开放
            String remoteHost = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            int serverPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
            if (Command.CommandType.getCommand(command.getName()).isOpen()
                    || (URIUtils.isLocalhost(remoteHost) && serverPort == serverContext.getConfig().getKvdbConfig().getManagerPort())) {
                serverContext.processCommand(sourceKey, message);
            } else {
                ctx.writeAndFlush(KVDBMessage.error(message.getId(), "un support command"));
            }
        }
    }

    private void clusterConfirm() {
        boolean confirmed = false;
        LOGGER.info("cluster confirming ... ");
        ClusterInfo localClusterInfo = serverContext.getClusterInfo();
        if (localClusterInfo.size() == 0) {
            return;
        }
        while (!confirmed) {
            Set<String> confirmedHosts = new HashSet<>();
            for (ClusterItem entry : localClusterInfo.getClusterItems()) {
                boolean ok = true;
                for (String url : entry.getURLs()) {
                    KVDBURI uri = new KVDBURI(url);
                    if (!(uri.isLocalhost() && uri.getPort() == serverContext.getConfig().getKvdbConfig().getPort())
                            && !confirmedHosts.contains(uri.getHost() + uri.getPort())) {
                        ok = confirmServer(localClusterInfo, uri);
                        if (ok) {
                            confirmedHosts.add(uri.getHost() + uri.getPort());
                        } else {
                            break;
                        }
                    }
                }
                confirmed = ok;
                if (!ok) {
                    break;
                }
            }
            if (!confirmed) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    LOGGER.error("sleep interrupted", e);
                }
            }
        }
        LOGGER.info("cluster confirmed");
    }

    private boolean confirmServer(ClusterInfo localClusterInfo, KVDBURI uri) {
        NettyClient client = null;
        try {
            LOGGER.info("cluster confirm {}", uri.getOrigin());
            CountDownLatch cdl = new CountDownLatch(1);
            client = new NettyClient(new ClientConfig(uri.getHost(), uri.getPort(), uri.getDatabase()), () -> cdl.countDown());
            cdl.await(CLUSTER_CONFIRM_TIME_OUT, TimeUnit.MILLISECONDS);
            Response response = client.send(KVDBMessage.clusterInfo());
            if (null == response || response.getCode() == Constants.ERROR) {
                String bi = BytesUtils.toString(response.getResult()[0].toBytes());
                LOGGER.error("cluster confirm {} error", BytesUtils.toString(response.getResult()[0].toBytes()));
                return false;
            }

            return localClusterInfo.match(serverContext.getConfig().getKvdbConfig().getPort(), uri, BinaryProtocol.decodeAs(response.getResult()[0].toBytes(), ClusterInfo.class));
        } catch (Exception e) {
            LOGGER.error("cluster confirm {} error", uri.getOrigin(), e);
            return false;
        } finally {
            if (null != client) {
                client.stop();
            }
        }
    }
}
