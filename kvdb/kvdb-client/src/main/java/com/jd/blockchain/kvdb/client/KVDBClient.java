package com.jd.blockchain.kvdb.client;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.kvdb.protocol.Constants;
import com.jd.blockchain.kvdb.protocol.KVDBURI;
import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import com.jd.blockchain.kvdb.protocol.client.NettyClient;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.kvdb.protocol.exception.KVDBTimeoutException;
import com.jd.blockchain.kvdb.protocol.proto.*;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.StringUtils;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * KVDB-SDK
 */
public class KVDBClient implements KVDBOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBClient.class);

    private ClientConfig config;
    /**
     * 保存当前所有连接，服务器地址加端口作为键值
     */
    private Map<String, NettyClient> clients = new HashMap<>();
    /**
     * 当前数据库实际操作者，当前选择数据库为单实例时使用{@link KVDBSingle}，否则{@link KVDBCluster}
     */
    private KVDBOperator operator;

    public KVDBClient(String url) throws KVDBException {
        this(new KVDBURI(url));
    }

    public KVDBClient(KVDBURI uri) throws KVDBException {
        this(new ClientConfig(uri.getHost(), uri.getPort(), uri.getDatabase()));
    }

    public KVDBClient(ClientConfig config) throws KVDBException {
        this.config = config;
        start();
    }

    /**
     * 创建客户端等待就绪状态，当配置数据库不为空时执行切换数据库操作
     */
    private void start() {
        clients.put(config.getHost() + config.getPort(), newNettyClient(config));
        if (!StringUtils.isEmpty(config.getDatabase())) {
            use(config.getDatabase());
        }
    }

    /**
     * 创建服务端连接，提供连接就绪回调接口。
     * 针对连接初始创建回调接口执行唤醒等待操作；
     * 针对服务掉线重连回调接口执行客户端重启操作。
     *
     * @param config
     * @return
     */
    private NettyClient newNettyClient(ClientConfig config) {
        CountDownLatch cdl = new CountDownLatch(1);
        NettyClient client = new NettyClient(config, () -> {
            if (cdl.getCount() > 0) {
                cdl.countDown();
            } else {
                if (!StringUtils.isEmpty(config.getDatabase())) {
                    clients.get(config.getHost() + config.getPort()).send(KVDBMessage.use(config.getDatabase()));
                }
            }
        });
        try {
            cdl.await(config.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new KVDBTimeoutException("new netty client timeout");
        }
        return client;
    }

    /**
     * 关闭客户端
     */
    public void close() {
        for (Map.Entry<String, NettyClient> entry : clients.entrySet()) {
            entry.getValue().stop();
        }
        clients.clear();
    }

    /**
     * 切换数据库，获取数据库配置信息，自动切换数据库单实例和集群操作模式
     *
     * @param db
     * @return
     * @throws KVDBException
     */
    protected synchronized DatabaseClusterInfo use(String db) throws KVDBException {
        if (StringUtils.isEmpty(db)) {
            throw new KVDBException("database is empty");
        }
        // 执行`use`命令，获取数据库配置信息
        Response response = clients.get(config.getHost() + config.getPort()).send(KVDBMessage.use(db));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }
        try {
            DatabaseClusterInfo info = BinaryProtocol.decodeAs(response.getResult()[0].toBytes(), DatabaseClusterInfo.class);
            config.setDatabase(db);
            if (info.isClusterMode()) {
                // 集群模式下，创建当前数据库所有服务节点连接，并执行切换数据库操作
                NettyClient[] selectedClients = new NettyClient[info.getClusterItem().getURLs().length];
                for (int i = 0; i < info.getClusterItem().getURLs().length; i++) {
                    KVDBURI uri = new KVDBURI(info.getClusterItem().getURLs()[i]);
                    if (uri.isLocalhost() && uri.getPort() == config.getPort()) {
                        selectedClients[i] = clients.get(uri.getHost() + uri.getPort());
                        continue;
                    }
                    NettyClient nettyClient;
                    if (!clients.containsKey(uri.getHost() + uri.getPort())) {
                        nettyClient = newNettyClient(new ClientConfig(uri.getHost(), uri.getPort(), uri.getDatabase()));
                        clients.put(uri.getHost() + uri.getPort(), nettyClient);
                    } else {
                        nettyClient = clients.get(uri.getHost() + uri.getPort());
                    }
                    response = nettyClient.send(KVDBMessage.use(uri.getDatabase()));
                    if (null == response) {
                        config.setDatabase(null);
                        throw new KVDBTimeoutException("time out");
                    } else if (response.getCode() == Constants.ERROR) {
                        config.setDatabase(null);
                        throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
                    }
                    selectedClients[i] = nettyClient;
                }
                operator = new KVDBCluster(selectedClients);
            } else {
                // 单实例模式下，无需再执行数据库切换操作，仅切换操作对象
                operator = new KVDBSingle(clients.get(config.getHost() + config.getPort()));
            }

            return info;
        } catch (Exception e) {
            LOGGER.error("use command error", e);
            String msg = e.getMessage();
            throw new KVDBException(!StringUtils.isEmpty(msg) ? msg : e.toString());
        }
    }

    /**
     * 创建数据库，仅支持本地连接管理服务端口进行操作
     *
     * @param parameter
     * @return
     * @throws KVDBException
     */
    protected boolean createDatabase(DatabaseBaseInfo parameter) throws KVDBException {
        Response response = clients.get(config.getHost() + config.getPort())
                .send(KVDBMessage.createDatabase(new Bytes(BinaryProtocol.encode(parameter, DatabaseBaseInfo.class))));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    /**
     * 开启数据库实例，仅支持本地连接管理服务端口进行操作
     *
     * @param database
     * @return
     * @throws KVDBException
     */
    protected boolean enableDatabase(String database) throws KVDBException {
        Response response = clients.get(config.getHost() + config.getPort())
                .send(KVDBMessage.enableDatabase(database));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    /**
     * 关闭数据库实例，仅支持本地连接管理服务端口进行操作
     *
     * @param database
     * @return
     * @throws KVDBException
     */
    protected boolean disableDatabase(String database) throws KVDBException {
        Response response = clients.get(config.getHost() + config.getPort())
                .send(KVDBMessage.disableDatabase(database));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    /**
     * 删除数据库实例，仅支持本地连接管理服务端口进行操作
     *
     * @param database
     * @return
     * @throws KVDBException
     */
    protected boolean dropDatabase(String database) throws KVDBException {
        Response response = clients.get(config.getHost() + config.getPort())
                .send(KVDBMessage.dropDatabase(database));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    /**
     * 服务器集群配置，仅支持本地连接管理服务端口进行操作
     *
     * @return
     * @throws KVDBException
     */
    protected ClusterItem[] clusterInfo() throws KVDBException {
        Response response = clients.get(config.getHost() + config.getPort()).send(KVDBMessage.clusterInfo());
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }
        ClusterInfo clusterInfo = BinaryProtocol.decodeAs(response.getResult()[0].toBytes(), ClusterInfo.class);
        return clusterInfo.getClusterItems();
    }

    /**
     * 当前服务器所有数据实例信息，仅支持本地连接管理服务端口进行操作
     *
     * @return
     * @throws KVDBException
     */
    protected DatabaseBaseInfo[] showDatabases() throws KVDBException {
        Response response = clients.get(config.getHost() + config.getPort()).send(KVDBMessage.showDatabases());
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return BinaryProtocol.decodeAs(response.getResult()[0].toBytes(), DatabaseBaseInfos.class).getBaseInfos();
    }

    @Override
    public boolean exists(Bytes key) throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.exists(key);
    }

    @Override
    public boolean[] exists(Bytes... keys) throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.exists(keys);
    }

    @Override
    public Bytes get(Bytes key) throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.get(key);
    }

    @Override
    public Bytes[] get(Bytes... keys) throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.get(keys);
    }

    @Override
    public boolean put(Bytes key, Bytes value) throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.put(key, value);
    }

    @Override
    public boolean batchBegin() throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.batchBegin();
    }

    @Override
    public boolean batchAbort() throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.batchAbort();
    }

    @Override
    public boolean batchCommit() throws KVDBException {
        if (StringUtils.isEmpty(config.getDatabase())) {
            throw new KVDBException("no database selected");
        }
        return operator.batchCommit();
    }
}
