package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.util.*;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockProposer;
import com.jd.blockchain.consensus.raft.consensus.BlockSerializer;
import com.jd.blockchain.consensus.raft.rpc.*;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.ledger.TransactionState;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RaftNodeServerServiceImpl implements RaftNodeServerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftNodeServerServiceImpl.class);
    private static final int MAX_SUBMIT_RETRY_TIMES = 3;

    private RaftNodeServer nodeServer;
    private BlockProposer proposer;
    private BlockSerializer serializer;

    private Disruptor<SubmitTx> submitTxDisruptor;
    private RingBuffer<SubmitTx> submitTxQueue;

    public RaftNodeServerServiceImpl(RaftNodeServer nodeServer, BlockProposer proposer, BlockSerializer serializer) {
        this.nodeServer = nodeServer;
        this.proposer = proposer;
        this.serializer = serializer;
        this.submitTxDisruptor = DisruptorBuilder.<SubmitTx>newInstance()
                .setRingBufferSize(nodeServer.getServerSettings().getRaftSettings().getDisruptorBufferSize())
                .setEventFactory(new SubmitTxFactory())
                .setThreadFactory(new NamedThreadFactory("Raft-SubmitTx-Disruptor-", true))
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .build();
        this.submitTxDisruptor.handleEventsWith(new SubmitTxHandler());
        this.submitTxDisruptor.setDefaultExceptionHandler(new LogExceptionHandler<>(getClass().getSimpleName()));
        this.submitTxQueue = this.submitTxDisruptor.start();
    }

    @Override
    public void handleSubmitTxRequest(SubmitTxRequest submitTxRequest, Closure done) {

        RpcResponseClosure responseClosure = (RpcResponseClosure) done;

        if (submitTxRequest.getTx() == null || submitTxRequest.getTx().length == 0) {
            done.run(new Status(RaftError.EREQUEST, "tx is empty"));
            return;
        }

        applyRequest(submitTxRequest, responseClosure, (req, closure) -> {
            int retryTimes = 0;
            try {
                final EventTranslator<SubmitTx> translator = (event, sequence) -> {
                    event.reset();
                    event.setDone(responseClosure);
                    event.setValues(submitTxRequest.getTx());
                };
                while (true) {
                    if (this.submitTxQueue.tryPublishEvent(translator)) {
                        break;
                    } else {
                        retryTimes++;
                        if (retryTimes > MAX_SUBMIT_RETRY_TIMES) {
                            LOGGER.error("node {} submit request is overload.", nodeServer.getNode().getNodeId());
                            responseClosure.run(new Status(RaftError.EBUSY, "node has too many tasks."));
                            return;
                        }
                        ThreadHelper.onSpinWait();
                    }
                }

            } catch (final Exception e) {
                LoggerUtils.errorIfEnabled(LOGGER, "fail to publish request:{} error", req, e);
                responseClosure.run(new Status(RaftError.EPERM, "publish tx error: %s", e.getMessage()));
            }
        });
    }

    @Override
    public void publishBlockEvent() {
        if (nodeServer.isLeader()) {
            this.submitTxQueue.publishEvent((event, sequence) -> event.setBlockEvent(true));
        }
    }

    @Override
    public void addParticipantNode(ParticipantNodeAddRequest request, Closure done) {
        applyRequest(request, (RpcResponseClosure) done, (req, closure) -> {
            PeerId changePeer = PeerId.parsePeer(String.format("%s:%d", request.getHost(), request.getPort()));
            nodeServer.getNode().addPeer(changePeer, done);

            boolean containPeer = nodeServer.getNode().listPeers().contains(changePeer);
            ((RpcResponseClosure) done).setResponse(containPeer ? RpcResponse.success(null) : RpcResponse.fail(-1, "add peers not contained"));
            done.run(Status.OK());
        });
    }

    @Override
    public void removeParticipantNode(ParticipantNodeRemoveRequest request, Closure done) {
        applyRequest(request, (RpcResponseClosure) done, (req, closure) -> {
            PeerId changePeer = PeerId.parsePeer(String.format("%s:%d", request.getHost(), request.getPort()));
            nodeServer.getNode().removePeer(changePeer, done);

            boolean containPeer = nodeServer.getNode().listPeers().contains(changePeer);
            ((RpcResponseClosure) done).setResponse(!containPeer ? RpcResponse.success(null) : RpcResponse.fail(-1, "contain remove peers"));
            done.run(Status.OK());
        });
    }

    @Override
    public void transferParticipantNode(ParticipantNodeTransferRequest request, Closure done) {
        applyRequest(request, (RpcResponseClosure) done, (req, closure) -> {
            PeerId removePeer = PeerId.parsePeer(String.format("%s:%d", request.getPreHost(), request.getPrePort()));
            PeerId addPeer = PeerId.parsePeer(String.format("%s:%d", request.getNewHost(), request.getNewPort()));
            List<PeerId> peerIds = nodeServer.getNode().listPeers();
            peerIds.remove(removePeer);
            peerIds.add(addPeer);

            nodeServer.getNode().changePeers(new Configuration(peerIds), done);

            boolean containPeer = nodeServer.getNode().listPeers().contains(addPeer);
            ((RpcResponseClosure) done).setResponse(containPeer ? RpcResponse.success(null) : RpcResponse.fail(-1, "add peers not contained"));
            done.run(Status.OK());
        });

    }


    private void applyRequest(Object txRequest, RpcResponseClosure done, BiConsumer<Object, RpcResponseClosure> consumer) {
        boolean isLeader = nodeServer.isLeader();

        if (!isLeader) {
            PeerId leader = nodeServer.getLeader();
            if (leader == null) {
                LoggerUtils.errorIfEnabled(LOGGER, "node:{} found leader is null", nodeServer.getNode().getNodeId());
                done.run(new Status(TransactionState.PARTICIPANT_DOES_NOT_EXIST.CODE, "leader not exist"));
            } else {
                LoggerUtils.debugIfEnabled(LOGGER, "node:{} found leader:{}, need redirect request to leader", nodeServer.getNode().getNodeId(), leader);
                redirectLeaderNode(leader, txRequest, done);
            }
            return;
        }

        try {
            consumer.accept(txRequest, done);
        } catch (Exception e) {
            LoggerUtils.errorIfEnabled(LOGGER, "consumer tx error", e);
            done.run(new Status(TransactionState.CONSENSUS_ERROR.CODE, e.getMessage()));
        }

    }

    private void redirectLeaderNode(PeerId leader, Object txRequest, RpcResponseClosure responseClosure) {
        try {
            nodeServer.getRpcClient().invokeAsync(leader.getEndpoint(), txRequest, (o, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("request to leader peer:{} got error", leader, throwable);
                    responseClosure.run(new Status(TransactionState.CONSENSUS_ERROR.CODE, throwable.getMessage()));
                    return;
                }
                LoggerUtils.debugIfEnabled(LOGGER, "request:{} to leader:{}, result is: {}", txRequest, leader, o);

                responseClosure.setResponse((RpcResponse) o);
                responseClosure.run(Status.OK());

            }, nodeServer.getServerSettings().getRaftNetworkSettings().getRpcRequestTimeoutMs());
        } catch (Exception e) {
            LOGGER.error("redirect request to leader error", e);
            responseClosure.run(new Status(TransactionState.CONSENSUS_ERROR.CODE, e.getMessage()));
        }
    }


    private static class SubmitTxFactory implements EventFactory<SubmitTx> {

        @Override
        public SubmitTx newInstance() {
            return new SubmitTx();
        }
    }

    private class SubmitTxHandler implements EventHandler<SubmitTx> {

        private final int maxTxNumberInBlock = nodeServer.getServerSettings().getMaxTxsPerBlock();
        private final int maxBytesPerBlock = nodeServer.getServerSettings().getMaxBlockBytes();
        private final AtomicInteger submitTxSize = new AtomicInteger(0);

        private final List<SubmitTx> submitTxList = new ArrayList<>(maxTxNumberInBlock);

        @Override
        public void onEvent(SubmitTx submitTx, long sequence, boolean endOfBatch) throws Exception {
            if (submitTx.isBlockEvent()) {
                if (!this.submitTxList.isEmpty()) {
                    proposalBlock(this.submitTxList);
                    reset();
                }
                submitTx.reset();
                return;
            }

            this.submitTxList.add(submitTx);
            submitTxSize.addAndGet(submitTx.getValues().length);

            if (this.submitTxList.size() >= maxTxNumberInBlock || submitTxSize.get() >= maxBytesPerBlock || endOfBatch) {
                proposalBlock(this.submitTxList);
                reset();
            }
        }

        private void reset() {
            for (final SubmitTx tx : submitTxList) {
                tx.reset();
            }
            this.submitTxList.clear();
            submitTxSize.set(0);
        }
    }

    private void proposalBlock(List<SubmitTx> submitTxList) {

        if (!proposer.canPropose()) {
            LoggerUtils.debugIfEnabled(LOGGER, "node {} can't propose block", nodeServer.getNode().getNodeId());
            for (SubmitTx submitTx : submitTxList) {
                submitTx.getDone().run(new Status(RaftError.ENEWLEADER, "node {} can't propose block", nodeServer.getNode().getNodeId()));
            }
            return;
        }

        LoggerUtils.debugIfEnabled(LOGGER, "node: {} begin proposal block, proposal tx size: {}", nodeServer.getNode().getNodeId(), submitTxList.size());

        List<byte[]> txList = submitTxList.stream().map(SubmitTx::getValues).collect(Collectors.toList());
        List<Closure> doneList = submitTxList.stream().map(SubmitTx::getDone).collect(Collectors.toList());

        try {
            final Task task = new Task();

            Block block = proposer.proposeBlock(txList);
            task.setData(ByteBuffer.wrap(serializer.serialize(block)));
            task.setDone(new BlockClosure(block, doneList));

            nodeServer.getNode().apply(task);
        } catch (Exception e) {
            LOGGER.error("proposal block error", e);
            //todo retry ?
            for (Closure closure : doneList) {
                runSubmitTxClosure(closure, i -> RpcResponse.fail(RaftError.UNKNOWN.getNumber(), e.getMessage()), 0);
            }
        }
    }

    private void runSubmitTxClosure(Closure closure, Function<Integer, RpcResponse> responseFunction, int index) {
        RpcResponseClosure submitTxDone = (RpcResponseClosure) closure;
        if (submitTxDone != null) {
            Utils.runClosureInThread(s -> {
                submitTxDone.setResponse(responseFunction.apply(index));
                submitTxDone.run(s);
            });
        }
    }

    public RaftNodeServer getNodeServer() {
        return nodeServer;
    }
}
