package com.jd.blockchain.consensus.mq.server;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ConsensusNodeNetwork;
import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.NodeNetworkTopology;
import com.jd.blockchain.consensus.mq.consumer.MQConsumer;
import com.jd.blockchain.consensus.mq.event.*;
import com.jd.blockchain.consensus.mq.event.binaryproto.*;
import com.jd.blockchain.consensus.mq.producer.MQProducer;
import com.jd.blockchain.consensus.mq.settings.MQNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MQNodeSettings;
import com.jd.blockchain.consensus.mq.settings.MQServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.MonitorService;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.runtime.RuntimeConstant;
import com.jd.blockchain.transaction.TxResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.StringUtils;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractMQMessageDispatcher implements MQMessageDispatcher {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractMQMessageDispatcher.class);
    protected volatile long latestHeight;
    protected volatile byte[] latestHash;
    protected MessageHandle messageHandle;
    protected String realmName;
    // 账本名
    protected StateMachineReplicate stateMachineReplicator;
    // 当前节点ID
    protected int nodeId;
    // 当前节点账户地址
    protected String nodeAddress;
    // 是否为Solo模式
    protected volatile boolean singleNode;
    // 交易监听
    protected MQConsumer txConsumer;
    // 交易结果发布
    protected MQProducer txResultProducer;
    // 区块提议
    protected MQProducer proposeProducer;
    // 提议监听
    protected MQConsumer proposeConsumer;
    // 非共识消息监听
    protected MQConsumer msgConsumer;
    // 非共识消息发布
    protected MQProducer msgProducer;
    // 非共识消息结果发布
    protected MQProducer msgResultProducer;

    // 区块交易最大数
    protected final int txSizePerBlock;
    // 最大提议时间间隔
    protected final long maxDelayMilliSecondsPerBlock;
    // 服务地址上报时间间隔
    protected final long pingMilliSeconds;
    // 待提议交易列表
    protected List<TxEvent> txMessages = new ArrayList<>();
    // 待提交交易锁
    protected ReentrantLock txLock = new ReentrantLock();
    // 服务拓扑
    protected Map<String, NodeNetworkAddress> serverTopology = new HashMap<>();

    protected String server;
    protected String txTopic;
    protected String txResultTopic;
    protected String msgTopic;
    protected String msgResultTopic;
    protected String proposeTopic;

    protected ScheduledThreadPoolExecutor proposeExecutor;
    protected ScheduledThreadPoolExecutor pingExecutor;
    protected ExecutorService resultExecutor;

    public AbstractMQMessageDispatcher(
            String server,
            MQServerSettings serverSettings,
            MessageHandle messageHandle,
            StateMachineReplicate stateMachineReplicator) {
        MQNodeSettings nodeSettings = serverSettings.getMQNodeSettings();
        this.nodeId = nodeSettings.getId();
        this.nodeAddress = nodeSettings.getAddress();
        if (!StringUtils.isEmpty(nodeSettings.getHost())) {
            serverTopology.put(
                    nodeAddress,
                    new ConsensusNodeNetwork(
                            nodeSettings.getHost(),
                            -1,
                            RuntimeConstant.getMonitorPort(),
                            false,
                            RuntimeConstant.isMonitorSecure()));
        }
        this.stateMachineReplicator = stateMachineReplicator;
        this.realmName = serverSettings.getRealmName();
        this.latestHeight = stateMachineReplicator.getLatestStateID(realmName);
        this.latestHash = stateMachineReplicator.getSnapshot(realmName, latestHeight).getSnapshot();
        this.txSizePerBlock = serverSettings.getBlockSettings().getTxSizePerBlock();
        this.maxDelayMilliSecondsPerBlock =
                serverSettings.getBlockSettings().getMaxDelayMilliSecondsPerBlock();
        this.pingMilliSeconds = serverSettings.getBlockSettings().getPingMilliseconds();
        this.singleNode = serverSettings.getConsensusSettings().getNodes().length == 1;
        this.messageHandle = messageHandle;

        MQNetworkSettings networkSettings = serverSettings.getConsensusSettings().getNetworkSettings();
        this.server = server;
        this.txTopic = networkSettings.getTxTopic();
        this.txResultTopic = networkSettings.getTxResultTopic();
        this.msgTopic = networkSettings.getMsgTopic();
        this.msgResultTopic = networkSettings.getMsgResultTopic();
        this.proposeTopic = networkSettings.getBlockTopic();

        // 根据不同MQ，实现差异化初始化
        init();
    }

    @Override
    public void connect() throws Exception {
        if (null != msgProducer) {
            msgProducer.connect();
        }
        if (null != msgResultProducer) {
            msgResultProducer.connect();
        }
        if (null != proposeProducer) {
            proposeProducer.connect();
        }
        if (null != txResultProducer) {
            txResultProducer.connect();
        }
        if (null != txConsumer) {
            txConsumer.connect(
                    msg -> {
                        try {
                            onTx(BinaryProtocol.decode(msg));
                        } catch (Exception e) {
                            LOGGER.warn("parse tx message error", e);
                        }
                    });
        }
        if (null != proposeConsumer) {
            proposeConsumer.connect(
                    propose -> {
                        try {
                            onPropose(BinaryProtocol.decode(propose));
                        } catch (Exception e) {
                            LOGGER.warn("parse propose message error", e);
                        }
                    });
        }
        if (null != msgConsumer) {
            msgConsumer.connect(
                    msg -> {
                        try {
                            onMessage(BinaryProtocol.decode(msg));
                        } catch (Exception e) {
                            LOGGER.warn("parse extend message error", e);
                        }
                    });
        }
    }

    @Override
    public synchronized void onPropose(ProposeEvent propose) {
        TxEvent[] txEvents = propose.getTxs();
        if (null == txEvents || txEvents.length == 0) {
            return;
        }
        if (!singleNode) {
            // 区块校验
            StateSnapshot snapshot =
                    stateMachineReplicator.getSnapshot(realmName, propose.getLatestHeight());
            if (null == snapshot || !Arrays.equals(propose.getLatestHash(), snapshot.getSnapshot())) {
                LOGGER.error("Inconsistent block {}: {}", nodeId, propose.getLatestHeight());
                try {
                    close();
                } catch (IOException e) {
                    LOGGER.error("error in dispatcher closing", e);
                }
                return;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exec block {}, tx size: {}", new ProposeMessage(propose), txEvents.length);
        }
        boolean isProposer = propose.getProposer() == nodeId;

        if (txEvents.length <= txSizePerBlock) {
            publishTxResult(isProposer, execBlock(txEvents, propose.getTimestamp()));
        } else {
            int p = txEvents.length / txSizePerBlock;
            int f = txEvents.length % txSizePerBlock;
            for (int i = 0; i < p + (f == 0 ? 0 : 1); i++) {
                List<TxEvent> txs = new ArrayList<>();
                for (int j = 0; j < txSizePerBlock; j++) {
                    int index = i * txSizePerBlock + j;
                    if (index == txEvents.length) {
                        break;
                    } else {
                        txs.add(txEvents[index]);
                    }
                }
                publishTxResult(
                        isProposer, execBlock(txs.toArray(new TxEvent[txs.size()]), propose.getTimestamp()));
            }
        }
    }

    private Map<String, AsyncFuture<byte[]>> execBlock(TxEvent[] txs, long timestamp) {
        Map<String, AsyncFuture<byte[]>> txResponseMap = new HashMap<>();
        // 使用MessageHandle处理
        MQConsensusMessageContext consensusContext =
                MQConsensusMessageContext.createInstance(realmName);
        String batchId = messageHandle.beginBatch(consensusContext);
        consensusContext.setBatchId(batchId);

        Set<String> txKeys = new HashSet<>();
        try {
            int messageId = 0;
            for (TxEvent tx : txs) {
                String txKey = tx.getKey();
                txKeys.add(txKey);
                byte[] txContent = tx.getMessage();
                AsyncFuture<byte[]> asyncFuture =
                        messageHandle.processOrdered(messageId++, txContent, consensusContext);
                txResponseMap.put(txKey, asyncFuture);
            }
            consensusContext.setTimestamp(timestamp);
            StateSnapshot snapshot = messageHandle.completeBatch(consensusContext);
            messageHandle.commitBatch(consensusContext);
            latestHeight = snapshot.getId();
            latestHash = snapshot.getSnapshot();
        } catch (Exception e) {
            TxResponseMessage responseMessage = new TxResponseMessage();
            responseMessage.setExecutionState(TransactionState.IGNORED_BY_BLOCK_FULL_ROLLBACK);
            byte[] encode = BinaryProtocol.encode(responseMessage, TransactionResponse.class);
            Iterator<String> iterator = txKeys.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                try {
                    if (txResponseMap.containsKey(key)) {
                        txResponseMap.get(key).cancel();
                    }
                } catch (Exception ex) {
                    LOGGER.error("cancel rollback future error", ex);
                }
                CompletableAsyncFuture<byte[]> asyncResult = new CompletableAsyncFuture();
                asyncResult.complete(encode);
                txResponseMap.put(key, asyncResult);
            }

            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, consensusContext);
        }

        return txResponseMap;
    }

    private void publishTxResult(boolean isProposer, Map<String, AsyncFuture<byte[]>> txResponseMap) {
        // 仅区块提议者推送交易结果
        if (isProposer && txResponseMap != null && !txResponseMap.isEmpty()) {
            for (Map.Entry<String, AsyncFuture<byte[]>> entry : txResponseMap.entrySet()) {
                final String txKey = entry.getKey();
                final byte[] result = entry.getValue().get();
                resultExecutor.execute(
                        () -> {
                            try {
                                // 通过消息队列发送交易执行结果
                                txResultProducer.publish(BinaryProtocol.encode(new TxResultMessage(txKey, result)));
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("publish tx result message, key: {}", txKey);
                                }
                            } catch (Exception e) {
                                LOGGER.error("publish tx result message exception", e);
                            }
                        });
            }
        }
    }

    @Override
    public void onMessage(ExtendEvent exMsg) throws Exception {
        byte[] msg = exMsg.getMessage();
        if (Arrays.equals(MonitorService.LOAD_MONITOR, msg)) {
            onTopology(exMsg.getKey());
        } else {
            UnOrderEvent unOrderEvent = BinaryProtocol.decode(msg);
            switch (unOrderEvent.getType()) {
                case PEER_ACTIVE:
                    onPeerActive(exMsg.getKey(), (PeerActiveEvent) unOrderEvent);
                    break;
                case PEER_INACTIVE:
                    onPeerInactive(exMsg.getKey(), (PeerInactiveEvent) unOrderEvent);
                    break;
                case PING:
                    onPing(exMsg.getKey(), (PingEvent) unOrderEvent);
                    break;
            }
        }
    }

    /**
     * 激活节点
     */
    private void onPeerActive(String key, PeerActiveEvent event) throws Exception {
        LOGGER.info("PEER_ACTIVE message {}", event.getAddress());
        singleNode = false;
        msgResultProducer.publish(BinaryProtocol.encode(ExtendResultMessage.success(key)));
    }

    /**
     * 移除节点
     */
    private void onPeerInactive(String key, PeerInactiveEvent event) throws Exception {
        synchronized (serverTopology) {
            LOGGER.info("PEER_INACTIVE message {}", event.getAddress());
            singleNode = false;
            msgResultProducer.publish(BinaryProtocol.encode(ExtendResultMessage.success(key)));
            serverTopology.remove(event.getAddress());
        }
    }

    /**
     * 心跳
     */
    private void onPing(String key, PingEvent event) {
        synchronized (serverTopology) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "PING message {} {}-{}:{}",
                        key,
                        event.getAddress(),
                        event.getNodeNetwork().getHost(),
                        event.getNodeNetwork().getMonitorPort());
            }
            serverTopology.put(event.getAddress(), event.getNodeNetwork());
        }
    }

    /**
     * 拓扑感知
     */
    private void onTopology(String key) throws Exception {
        synchronized (serverTopology) {
            if (serverTopology.size() > 0) {
                LOGGER.info("TOPOLOGY message");
                byte[] topology =
                        BinaryProtocol.encode(
                                new NodeNetworkTopology(
                                        serverTopology.values().toArray(new NodeNetworkAddress[0])));
                msgResultProducer.publish(
                        BinaryProtocol.encode(ExtendResultMessage.success(key, topology)));
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (null != txConsumer) {
            txConsumer.close();
        }
        if (null != proposeConsumer) {
            proposeConsumer.close();
        }
        if (null != msgConsumer) {
            msgConsumer.close();
        }

        if (null != msgProducer) {
            msgProducer.close();
        }
        if (null != proposeProducer) {
            proposeProducer.close();
        }
        if (null != txResultProducer) {
            txResultProducer.close();
        }
        if (null != msgResultProducer) {
            msgResultProducer.close();
        }

        if (null != proposeExecutor) {
            proposeExecutor.shutdown();
        }
        if (null != resultExecutor) {
            resultExecutor.shutdown();
        }
        if (null != pingExecutor) {
            pingExecutor.shutdown();
        }
    }

    @Override
    public void run() {
        try {
            if (null != proposeConsumer) {
                proposeConsumer.start();
            }
            if (null != txConsumer) {
                txConsumer.start();
            }
            if (null != msgConsumer) {
                msgConsumer.start();
            }
            // 开启定时提议
            if (null != proposeProducer && maxDelayMilliSecondsPerBlock > 0) {
                this.proposeExecutor.scheduleWithFixedDelay(
                        () -> onProposeTime(),
                        maxDelayMilliSecondsPerBlock * (nodeId + 1),
                        maxDelayMilliSecondsPerBlock,
                        TimeUnit.MILLISECONDS);
            }
            // Ping
            if (null != msgProducer && pingMilliSeconds > 0) {
                this.pingExecutor.scheduleWithFixedDelay(
                        () -> {
                            if (serverTopology.containsKey(nodeAddress)) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("ping");
                                }
                                try {
                                    msgProducer.publish(
                                            BinaryProtocol.encode(
                                                    new ExtendMessage(
                                                            UUID.randomUUID().toString(),
                                                            BinaryProtocol.encode(
                                                                    new PingMessage(
                                                                            ExtendType.PING,
                                                                            nodeAddress,
                                                                            serverTopology.get(nodeAddress))))));
                                } catch (Exception e) {
                                    LOGGER.error("ping error", e);
                                }
                            }
                        },
                        1000,
                        pingMilliSeconds,
                        TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            LOGGER.error("consumers start error", e);
        }
    }

    /**
     * 根据MQ选型，差异化初始生产者、消费者
     */
    abstract void init();

    @Override
    public void propose() {
    }

    @Override
    public void propose(List<TxEvent> txs) {
    }
}
