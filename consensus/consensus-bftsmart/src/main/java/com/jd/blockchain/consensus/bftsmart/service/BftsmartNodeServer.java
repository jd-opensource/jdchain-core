package com.jd.blockchain.consensus.bftsmart.service;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import bftsmart.consensus.app.BatchAppResultImpl;
import bftsmart.consensus.app.ComputeCode;
import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.*;
import com.alibaba.fastjson.JSON;
import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.BlockStateSnapshot;
import com.jd.blockchain.consensus.service.*;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.runtime.RuntimeConstant;
import com.jd.blockchain.transaction.MonitorService;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.blockchain.utils.StringUtils;
import com.jd.blockchain.utils.net.NetworkAddress;
import com.jd.blockchain.utils.serialize.binary.BinarySerializeUtils;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jd.blockchain.consensus.ConsensusManageService;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartTopology;
import com.jd.blockchain.utils.PropertiesUtils;
import com.jd.blockchain.utils.concurrent.AsyncFuture;
import com.jd.blockchain.utils.io.BytesUtils;
import bftsmart.reconfiguration.util.HostsConfig;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import org.springframework.util.NumberUtils;

public class BftsmartNodeServer extends DefaultRecoverable implements NodeServer {

    private static Logger LOGGER = LoggerFactory.getLogger(BftsmartNodeServer.class);

//    private static final String DEFAULT_BINDING_HOST = "0.0.0.0";

    private List<StateHandle> stateHandles = new CopyOnWriteArrayList<>();

    private final Map<String, BftsmartConsensusMessageContext> contexts = Collections.synchronizedMap(new LRUMap<>(1024));

    private volatile Status status = Status.STOPPED;

    private final Object mutex = new Object();

    private volatile ServiceReplica replica;

    private StateMachineReplicate stateMachineReplicate;

    private ServerSettings serverSettings;

    private BftsmartConsensusManageService manageService;


    private volatile BftsmartTopology topology;

    private volatile BftsmartTopology outerTopology;

    private volatile BftsmartConsensusSettings setting;

    private TOMConfiguration tomConfig;

    private TOMConfiguration outerTomConfig;

    private HostsConfig hostsConfig;

    private Properties systemConfig;

    private MessageHandle messageHandle;

    private String providerName;

    private String realmName;

    private int serverId;

    private long latestStateId;

    private View latestView;

    private List<NodeNetwork> consensusAddresses = new ArrayList<>();

    private final Lock batchHandleLock = new ReentrantLock();

    private volatile InnerStateHolder stateHolder;

    private long timeTolerance = -1L;

    public BftsmartNodeServer() {

    }

    public BftsmartNodeServer(ServerSettings serverSettings, MessageHandle messageHandler, StateMachineReplicate stateMachineReplicate) {
        this.serverSettings = serverSettings;
        this.realmName = serverSettings.getRealmName();
        //used later
        this.stateMachineReplicate = stateMachineReplicate;
        this.latestStateId = stateMachineReplicate.getLatestStateID(realmName);
        this.stateHolder = new InnerStateHolder(latestStateId - 1);
        this.messageHandle = messageHandler;
        createConfig();
        serverId = findServerId();
        initConfig(serverId, systemConfig, hostsConfig);
        this.manageService = new BftsmartConsensusManageService(this);
        this.timeTolerance = tomConfig.getTimeTolerance();
    }

    protected int findServerId() {
        int serverId = 0;

        String host = ((BftsmartNodeSettings)serverSettings.getReplicaSettings()).getNetworkAddress().getHost();
        int port = ((BftsmartNodeSettings)serverSettings.getReplicaSettings()).getNetworkAddress().getPort();
        for (int i : hostsConfig.getHostsIds()) {

            if (hostsConfig.getHost(i).equals(host) && hostsConfig.getPort(i) == port) {
                serverId = i;
                break;
            }
        }

        return serverId;
    }

    public int getServerId() {
        return serverId;
    }

    protected void createConfig() {
        int monitorPort = RuntimeConstant.MONITOR_PORT.get();
        setting = ((BftsmartServerSettings) serverSettings).getConsensusSettings();

        List<HostsConfig.Config> configList = new ArrayList<>();

        NodeSettings[] nodeSettingsArray = setting.getNodes();
        for (NodeSettings nodeSettings : nodeSettingsArray) {
            BftsmartNodeSettings node = (BftsmartNodeSettings)nodeSettings;
            configList.add(new HostsConfig.Config(node.getId(), node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort(), -1));
            consensusAddresses.add(new NodeNetwork(node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort(), -1));
        }

        consensusAddresses.get(3);

        //create HostsConfig instance based on consensus realm nodes
        hostsConfig = new HostsConfig(configList.toArray(new HostsConfig.Config[configList.size()]));

        systemConfig = PropertiesUtils.createProperties(setting.getSystemConfigs());

        return;
    }

    protected void initConfig(int id, Properties systemsConfig, HostsConfig hostConfig) {
        HostsConfig outerHostConfig = BinarySerializeUtils.deserialize(BinarySerializeUtils.serialize(hostConfig));
        Properties sysConfClone = (Properties)systemsConfig.clone();
        int port = hostConfig.getPort(id);
//        hostConfig.add(id, DEFAULT_BINDING_HOST, port);

        //if peer-startup.sh set up the -DhostIp=xxx, then get it;
        String preHostPort = System.getProperty("hostPort");
        if(!StringUtils.isEmpty(preHostPort)){
            port = NumberUtils.parseNumber(preHostPort, Integer.class);
            LOGGER.info("###peer-startup.sh###,set up the -DhostPort="+port);
        }
        int monitorPort = RuntimeConstant.MONITOR_PORT.get();
        String preHostIp = System.getProperty("hostIp");
        if(!StringUtils.isEmpty(preHostIp)){
            hostConfig.add(id, preHostIp, port, monitorPort);
            LOGGER.info("###peer-startup.sh###,set up the -DhostIp="+preHostIp);
        }

        this.tomConfig = new TOMConfiguration(id, systemsConfig, hostConfig, outerHostConfig);

        this.latestView = new View(setting.getViewId(), tomConfig.getInitialView(), tomConfig.getF(), consensusAddresses.toArray(new NodeNetwork[consensusAddresses.size()]));

        this.outerTomConfig = new TOMConfiguration(id, sysConfClone, outerHostConfig);

    }

    @Override
    public ConsensusManageService getConsensusManageService() {
        return manageService;
    }

    @Override
    public ServerSettings getSettings() {
        return serverSettings;
    }

    @Override
    public String getProviderName() {
        return BftsmartConsensusProvider.NAME;
    }

    // 由于节点动态入网的原因，共识的配置环境是随时可能变化的，需要每次get时从replica动态读取
    public TOMConfiguration getTomConfig() {
       return outerTomConfig;
    }

    public int getId() {
        return tomConfig.getProcessId();
    }

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("ReplicaID is negative!");
        }
        this.tomConfig.setProcessId(id);
        this.outerTomConfig.setProcessId(id);

    }

    // 注意：该方法获得的共识环境为节点启动时从账本里读取的共识环境，如果运行过程中发生了节点动态入网，该环境没有得到更新
    public BftsmartConsensusSettings getConsensusSetting() {
        return setting;
    }

//    public BftsmartTopology getTopology() {
//        if (outerTopology != null) {
//            return outerTopology;
//        }
//        return new BftsmartTopology(replica.getReplicaContext().getCurrentView());
//    }

    public BftsmartTopology getTopology() {
        if (!isRunning()) {
            return null;
        }
        return getOuterTopology();
    }

    private BftsmartTopology getOuterTopology() {
        View currView = this.replica.getReplicaContext().getCurrentView();
        int id = currView.getId();
        int f = currView.getF();
        int[] processes = currView.getProcesses();
        NodeNetwork[] addresses = new NodeNetwork[processes.length];
        for (int i = 0; i < processes.length; i++) {
            int pid = processes[i];
            if (serverId == pid) {
                addresses[i] = new NodeNetwork(getTomConfig().getHost(pid), getTomConfig().getPort(pid),
                        getTomConfig().getMonitorPort(pid));
            } else {
                addresses[i] = currView.getAddress(pid);
            }
        }
        View returnView = new View(id, processes, f, addresses);

        for (int i = 0; i < returnView.getProcesses().length; i++) {
            LOGGER.info("[BftsmartNodeServer.getOuterTopology] PartiNode id = {}, host = {}, port = {}", returnView.getProcesses()[i],
                    returnView.getAddress(returnView.getProcesses()[i]).getHost(), returnView.getAddress(returnView.getProcesses()[i]).getConsensusPort());
        }
        this.outerTopology = new BftsmartTopology(returnView);

        return outerTopology;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        if (Arrays.equals(MonitorService.LOAD_MONITOR, bytes)) {
            // 获取加载管理端口的信息
            View currView = this.replica.getReplicaContext().getCurrentView();
            Map<Integer, NodeNetwork> addresses = currView.getAddresses();
            TreeMap<Integer, NetworkAddress> tree = new TreeMap<>();
            for (Map.Entry<Integer, NodeNetwork> entry : addresses.entrySet()) {
                tree.put(entry.getKey(), networkAddress(entry.getValue()));
            }
            Collection<NetworkAddress> nodeNetworks = tree.values();
            String jsonString = JSON.toJSONString(new ArrayList<>(nodeNetworks));
            return jsonString.getBytes(StandardCharsets.UTF_8);
        }

        return messageHandle.processUnordered(bytes).get();
    }

    private NetworkAddress networkAddress(NodeNetwork nodeNetwork) {
        // 只需要管理口信息即可
        return new NetworkAddress(nodeNetwork.getHost(), nodeNetwork.getMonitorPort());
    }

    /**
     *
     *  Only block, no reply， used by state transfer when peer start
     *
     */
    private void block(List<byte[]> manageConsensusCmds, long timestamp) {
        BftsmartConsensusMessageContext context = BftsmartConsensusMessageContext.createInstance(realmName, timestamp);
        String batchId = messageHandle.beginBatch(context);
        context.setBatchId(batchId);
        try {
            StateSnapshot preStateSnapshot = messageHandle.getStateSnapshot(context);
            if (preStateSnapshot instanceof BlockStateSnapshot) {
                BlockStateSnapshot preBlockStateSnapshot = (BlockStateSnapshot)preStateSnapshot;
                long preBlockTimestamp = preBlockStateSnapshot.getTimestamp();
                if (timestamp < preBlockTimestamp && (preBlockTimestamp - timestamp) > timeTolerance) {
                    // 打印错误信息
                    LOGGER.warn("The time[{}] of the last block is mismatch with the current[{}] for time tolerance[{}] !!!",
                            preBlockTimestamp, timestamp, timeTolerance);
                    // 回滚该操作
                    messageHandle.rollbackBatch(TransactionState.CONSENSUS_TIMESTAMP_ERROR.CODE, context);
                    return;
                }
            }

            int msgId = 0;
            for (byte[] txContent : manageConsensusCmds) {
                messageHandle.processOrdered(msgId++, txContent, context);
            }
            messageHandle.completeBatch(context);
            messageHandle.commitBatch(context);
        } catch (Exception e) {
            // todo 需要处理应答码 404
            LOGGER.error("Error occurred while processing ordered messages! --" + e.getMessage(), e);
            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, context);
        }
    }

    /**
     *
     *  Local peer has cid diff with remote peer, used by state transfer when peer start
     *
     */
    private byte[][] appExecuteDiffBatch(byte[][] commands, MessageContext[] msgCtxs) {

        int manageConsensusId = msgCtxs[0].getConsensusId();
        long timestamp = msgCtxs[0].getTimestamp();
        List<byte[]> manageConsensusCmds = new ArrayList<>();

        int index = 0;
        for (MessageContext msgCtx : msgCtxs) {
            if (msgCtx.getConsensusId() == manageConsensusId) {
                manageConsensusCmds.add(commands[index]);
            } else {
                // 达到结块标准，需要进行结块并应答
                block(manageConsensusCmds, timestamp);
                // 重置链表和共识ID
                manageConsensusCmds = new ArrayList<>();
                manageConsensusId = msgCtx.getConsensusId();
                timestamp = msgCtx.getTimestamp();
                manageConsensusCmds.add(commands[index]);
            }
            index++;
        }
        // 结束时，肯定有最后一个结块请求未处理
        if (!manageConsensusCmds.isEmpty()) {
            block(manageConsensusCmds, timestamp);
        }
        return null;

    }

    /**
     *
     *  Invoked by state transfer when peer start
     *
     */
    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs, boolean fromConsensus) {

        // Not from consensus outcomes， from state transfer
        if (!fromConsensus) {
            return appExecuteDiffBatch(commands, msgCtxs);
        }

        return null;
    }

    /**
     *
     *  From consensus outcomes, do nothing now
     *  The operation of executing the batch was moved to the consensus stage 2 and 3, in order to guaranteed ledger consistency
     */
    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs, boolean fromConsensus, List<ReplyContextMessage> replyList) {

//        if (replyList == null || replyList.size() == 0) {
//            throw new IllegalArgumentException();
//        }
//        // todo 此部分需要重新改造
//        /**
//         * 默认BFTSmart接口提供的commands是一个或多个共识结果的顺序集合
//         * 根据共识的规定，目前的做法是将其根据msgCtxs的内容进行分组，每组都作为一个结块标识来处理
//         * 从msgCtxs可以获取对应commands的分组情况
//         */
//        int manageConsensusId = msgCtxs[0].getConsensusId();
//        List<byte[]> manageConsensusCmds = new ArrayList<>();
//        List<ReplyContextMessage> manageReplyMsgs = new ArrayList<>();
//
//        int index = 0;
//        for (MessageContext msgCtx : msgCtxs) {
//            if (msgCtx.getConsensusId() == manageConsensusId) {
//                manageConsensusCmds.add(commands[index]);
//                manageReplyMsgs.add(replyList.get(index));
//            } else {
//                // 达到结块标准，需要进行结块并应答
//                blockAndReply(manageConsensusCmds, manageReplyMsgs);
//                // 重置链表和共识ID
//                manageConsensusCmds = new ArrayList<>();
//                manageReplyMsgs = new ArrayList<>();
//                manageConsensusId = msgCtx.getConsensusId();
//                manageConsensusCmds.add(commands[index]);
//                manageReplyMsgs.add(replyList.get(index));
//            }
//            index++;
//        }
//        // 结束时，肯定有最后一个结块请求未处理
//        if (!manageConsensusCmds.isEmpty()) {
//            blockAndReply(manageConsensusCmds, manageReplyMsgs);
//        }
        return null;
    }

    /**
     *
     *  Block and reply are moved to consensus completion stage
     *
     */
    private void blockAndReply(List<byte[]> manageConsensusCmds, List<ReplyContextMessage> replyList) {
//        consensusBatchId = messageHandle.beginBatch(realmName);
//        List<AsyncFuture<byte[]>> asyncFutureLinkedList = new ArrayList<>(manageConsensusCmds.size());
//        try {
//            int msgId = 0;
//            for (byte[] txContent : manageConsensusCmds) {
//                AsyncFuture<byte[]> asyncFuture = messageHandle.processOrdered(msgId++, txContent, realmName, consensusBatchId);
//                asyncFutureLinkedList.add(asyncFuture);
//            }
//            messageHandle.completeBatch(realmName, consensusBatchId);
//            messageHandle.commitBatch(realmName, consensusBatchId);
//        } catch (Exception e) {
//            // todo 需要处理应答码 404
//        	LOGGER.error("Error occurred while processing ordered messages! --" + e.getMessage(), e);
//            messageHandle.rollbackBatch(realmName, consensusBatchId, TransactionState.CONSENSUS_ERROR.CODE);
//        }
//
//        // 通知线程单独处理应答
//        notifyReplyExecutors.execute(() -> {
//            // 应答对应的结果
//            int replyIndex = 0;
//            for(ReplyContextMessage msg : replyList) {
//                msg.setReply(asyncFutureLinkedList.get(replyIndex).get());
//                TOMMessage request = msg.getTomMessage();
//                ReplyContext replyContext = msg.getReplyContext();
//                request.reply = new TOMMessage(replyContext.getId(), request.getSession(), request.getSequence(),
//                        request.getOperationId(), msg.getReply(), replyContext.getCurrentViewId(),
//                        request.getReqType());
//
//                if (replyContext.getNumRepliers() > 0) {
//                    bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) sending reply to "
//                            + request.getSender() + " with sequence number " + request.getSequence()
//                            + " and operation ID " + request.getOperationId() + " via ReplyManager");
//                    replyContext.getRepMan().send(request);
//                } else {
//                    bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) sending reply to "
//                            + request.getSender() + " with sequence number " + request.getSequence()
//                            + " and operation ID " + request.getOperationId());
//                    replyContext.getReplier().manageReply(request, msg.getMessageContext());
//                }
//                replyIndex++;
//            }
//        });
    }

    /**
     * Used by consensus write phase, pre compute new block hash
     * @param cid
     * 	      当前正在进行的共识ID；
     * @param commands
     *        请求列表
     */
    @Override
    public BatchAppResultImpl preComputeAppHash(int cid, byte[][] commands, long timestamp) {
        List<AsyncFuture<byte[]>> asyncFutureLinkedList = new ArrayList<>(commands.length);
        List<byte[]> responseLinkedList = new ArrayList<>();
        StateSnapshot newStateSnapshot, preStateSnapshot, genisStateSnapshot;
        BatchAppResultImpl result;
        String batchId = "";
        int msgId = 0;
        byte[] cidBytes = BytesUtils.toBytes(cid);
        batchHandleLock.lock();
        try {
            if(commands.length == 0) {
                // 没有要做预计算的消息，直接组装结果返回
                result = BatchAppResultImpl.createFailure(responseLinkedList, cidBytes, batchId, cidBytes);
            } else {
                BftsmartConsensusMessageContext context = BftsmartConsensusMessageContext.createInstance(realmName, timestamp);
                batchId = messageHandle.beginBatch(context);
                context.setBatchId(batchId);
                contexts.put(batchId, context);
                stateHolder.batchingID = batchId;
                // 获取前置区块快照状态
                preStateSnapshot = messageHandle.getStateSnapshot(context);
                if (preStateSnapshot instanceof BlockStateSnapshot) {
                    BlockStateSnapshot preBlockStateSnapshot = (BlockStateSnapshot)preStateSnapshot;
                    long preBlockTimestamp = preBlockStateSnapshot.getTimestamp();
                    if (timestamp < preBlockTimestamp && (preBlockTimestamp - timestamp) > timeTolerance) {
                        // 打印错误信息
                        LOGGER.warn("The time[{}] of the last block is mismatch with the current[{}] for time tolerance[{}] !!!",
                                preBlockTimestamp, timestamp, timeTolerance);
                        // 设置返回的应答信息
                        for (byte[] command : commands) {
                            // 状态设置为共识错误
                            responseLinkedList.add(createAppResponse(command, TransactionState.CONSENSUS_TIMESTAMP_ERROR));
                        }
                        // 将该状态设置为未执行
                        stateHolder.setComputeStatus(PreComputeStatus.UN_EXECUTED);
                        // 回滚该操作
                        messageHandle.rollbackBatch(TransactionState.CONSENSUS_TIMESTAMP_ERROR.CODE, context);
                        // 返回成功，但需要设置当前的状态
                        return BatchAppResultImpl.createSuccess(responseLinkedList, cidBytes, batchId, cidBytes);
                    } else {
                        LOGGER.info("Last block's timestamp = {}, current timestamp = {}, time tolerance = {} !",
                                preBlockTimestamp, timestamp, timeTolerance);
                    }
                }

                // 创世区块的状态快照
                genisStateSnapshot = messageHandle.getGenesisStateSnapshot(context);
                for (byte[] txContent : commands) {
                    AsyncFuture<byte[]> asyncFuture = messageHandle.processOrdered(msgId++, txContent, context);
                    asyncFutureLinkedList.add(asyncFuture);
                }

                newStateSnapshot = messageHandle.completeBatch(context);
                for (AsyncFuture<byte[]> asyncFuture : asyncFutureLinkedList) {
                    responseLinkedList.add(asyncFuture.get());
                }

                result = BatchAppResultImpl.createSuccess(responseLinkedList, newStateSnapshot.getSnapshot(), batchId,
                        genisStateSnapshot.getSnapshot());
            }
        } catch (BlockRollbackException e) {
            LOGGER.error("Error occurred while pre compute app! --" + e.getMessage(), e);
            for (byte[] command : commands) {
                responseLinkedList.add(createAppResponse(command, e.getState()));
            }
            result = BatchAppResultImpl.createFailure(responseLinkedList, cidBytes, batchId, cidBytes);
        } catch (Exception e) {
            LOGGER.error("Error occurred while pre compute app! --" + e.getMessage(), e);
            for (byte[] command : commands) {
                responseLinkedList.add(createAppResponse(command, TransactionState.IGNORED_BY_BLOCK_FULL_ROLLBACK));
            }
            result = BatchAppResultImpl.createFailure(responseLinkedList, cidBytes, batchId, cidBytes);
        } finally {
            batchHandleLock.unlock();
        }

        return result;
    }

    // Block full rollback responses, generated in pre compute phase, due to tx exception
    private byte[] createAppResponse(byte[] command, TransactionState transactionState) {
        TransactionRequest txRequest = BinaryProtocol.decode(command);

        TxResponseMessage resp = new TxResponseMessage(txRequest.getTransactionHash());

        resp.setExecutionState(transactionState);

        return BinaryProtocol.encode(resp, TransactionResponse.class);
    }

    @Override
    public List<byte[]> updateAppResponses(List<byte[]> asyncResponseLinkedList, byte[] commonHash, boolean isConsistent) {
        List<byte[]> updatedResponses = new ArrayList<>();
        TxResponseMessage resp = null;
        for(int i = 0; i < asyncResponseLinkedList.size(); i++) {
            TransactionResponse txResponse = BinaryProtocol.decode(asyncResponseLinkedList.get(i));
            if (isConsistent) {
                resp = new TxResponseMessage(txResponse.getContentHash());
            } else {
                resp = new TxResponseMessage(new HashDigest(commonHash));
            }
            resp.setExecutionState(TransactionState.IGNORED_BY_BLOCK_FULL_ROLLBACK);
            updatedResponses.add(BinaryProtocol.encode(resp, TransactionResponse.class));
        }
        return updatedResponses;
    }
    /**
     *
     *  Decision has been made at the consensus stage， commit block
     *
     */
    @Override
    public void preComputeAppCommit(int cid, String batchId) {
        batchHandleLock.lock();
        try {
            BftsmartConsensusMessageContext context = contexts.get(batchId);
            if (context == null) {
                throw new NoSuchElementException("no element by " + batchId);
            }
            // 判断该CID是否执行过
            PreComputeStatus computeStatus = stateHolder.getComputeStatus();
            if (computeStatus == PreComputeStatus.UN_EXECUTED) {
                // 当前CID并未真正操作账本，因此无需做处理
                stateHolder.reset();
                return;
            }

//            long lastCid = stateHolder.lastCid;
//            if (cid <= lastCid) {
//                // 表示该CID已经执行过，不再处理
//                return;
//            }
//            stateHolder.setLastCid(cid);
            String batchingID = stateHolder.batchingID;
            stateHolder.reset();
            if (!StringUtils.isEmpty(batchId) && batchId.equals(batchingID)) {
                messageHandle.commitBatch(context);
            }
        } catch (BlockRollbackException e) {
            LOGGER.error("Error occurred while pre compute commit --" + e.getMessage(), e);
            throw e;
        } finally {
            batchHandleLock.unlock();
        }
    }

    /**
     *
     *  Consensus write phase will terminate, new block hash values are inconsistent, rollback block
     *
     */
    @Override
    public void preComputeAppRollback(int cid, String batchId) {
        batchHandleLock.lock();
        try {
            BftsmartConsensusMessageContext context = contexts.get(batchId);
            if (context == null) {
                throw new NoSuchElementException("no element by " + batchId);
            }
//            long lastCid = stateHolder.lastCid;
//            if (cid <= lastCid) {
//                // 表示该CID已经执行过，不再处理
//                return;
//            }
//            stateHolder.setLastCid(cid);
            // 判断该CID是否执行过
            PreComputeStatus computeStatus = stateHolder.getComputeStatus();
            if (computeStatus == PreComputeStatus.UN_EXECUTED) {
                // 当前CID并未真正操作账本，因此无需做处理
                stateHolder.reset();
                return;
            }
            String batchingID = stateHolder.batchingID;
            stateHolder.reset();
            LOGGER.debug("Rollback of operations that cause inconsistencies in the ledger");
            if (!StringUtils.isEmpty(batchId) && batchId.equals(batchingID)) {
                messageHandle.rollbackBatch(TransactionState.IGNORED_BY_BLOCK_FULL_ROLLBACK.CODE, context);
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while pre compute rollback --" + e.getMessage(), e);
            throw e;
        } finally {
            batchHandleLock.unlock();
        }
    }

    //notice
    @Override
    public byte[] getSnapshot() {
        LOGGER.debug("------- GetSnapshot...[replica.id=" + this.getId() + "]");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BytesUtils.writeInt(stateHandles.size(), out);
        for (StateHandle stateHandle : stateHandles) {
            // TODO: 测试代码；
            return stateHandle.takeSnapshot();
        }
        return out.toByteArray();
    }

    @Override
    public void installSnapshot(byte[] snapshot) {
//        System.out.println("Not implement!");
    }

    @Override
    public void start() {
        if (this.getId() < 0) {
            throw new IllegalStateException("Unset server node ID！");
        }
        LOGGER.info("=============================== Start replica ===================================");

        if (status != Status.STOPPED) {
            return;
        }
        synchronized (mutex) {
            if (status != Status.STOPPED) {
                return;
            }
            status = Status.STARTING;

            try {
                LOGGER.info("Start replica...[ID=" + getId() + "]");
//                this.replica = new ServiceReplica(tomConfig, this, this);
                this.replica = new ServiceReplica(tomConfig, this, this, (int)latestStateId -1, latestView);
                this.topology = new BftsmartTopology(replica.getReplicaContext().getCurrentView());
//                initOutTopology();
                status = Status.RUNNING;
//                createProxyClient();
                LOGGER.info(
                        "=============================== Replica started success! ===================================");
            } catch (RuntimeException e) {
                status = Status.STOPPED;
                throw e;
            }
        }

    }

    @Override
    public void stop() {
        if (status != Status.RUNNING) {
            return;
        }
        synchronized (mutex) {
            if (status != Status.RUNNING) {
                return;
            }
            status = Status.STOPPING;

            try {
                ServiceReplica rep = this.replica;
                if (rep != null) {
                    LOGGER.debug("Stop replica...[ID=" + rep.getId() + "]");
                    this.replica = null;
                    this.topology = null;

                    rep.kill();
                    LOGGER.debug("Replica had stopped! --[ID=" + rep.getId() + "]");
                }
            } finally {
                status = Status.STOPPED;
            }
        }
    }

    enum Status {

        STARTING,

        RUNNING,

        STOPPING,

        STOPPED

    }

    private static class InnerStateHolder {

        private long lastCid;

        private long currentCid = -1L;

        private String batchingID = "";

        /**
         * 预计算状态
         *         默认为已计算
         */
        private PreComputeStatus computeStatus = PreComputeStatus.EXECUTED;

        public InnerStateHolder(long lastCid) {
            this.lastCid = lastCid;
        }

        public InnerStateHolder(long lastCid, long currentCid) {
            this.lastCid = lastCid;
            this.currentCid = currentCid;
        }

        public long getLastCid() {
            return lastCid;
        }

        public void setLastCid(long lastCid) {
            this.lastCid = lastCid;
        }

        public long getCurrentCid() {
            return currentCid;
        }

        public void setCurrentCid(long currentCid) {
            this.currentCid = currentCid;
        }

        public String getBatchingID() {
            return batchingID;
        }

        public void setBatchingID(String batchingID) {
            this.batchingID = batchingID;
        }

        public PreComputeStatus getComputeStatus() {
            return computeStatus;
        }

        public void setComputeStatus(PreComputeStatus computeStatus) {
            this.computeStatus = computeStatus;
        }

        public void reset() {
            currentCid = -1;
            batchingID = "";
            computeStatus = PreComputeStatus.EXECUTED;
        }
    }

    private enum PreComputeStatus {

        /**
         * 已执行
         *
         */
        EXECUTED,

        /**
         * 未执行
         *
         */
        UN_EXECUTED,
    }
}
