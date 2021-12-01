package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Utils;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockCommittedException;
import com.jd.blockchain.consensus.raft.consensus.BlockCommitter;
import com.jd.blockchain.consensus.raft.consensus.BlockSerializer;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.core.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftConsensusStateMachine extends StateMachineAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConsensusStateMachine.class);

    private BlockCommitter committer;
    private BlockSerializer serializer;
    private LedgerRepository ledgerRepository;

    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private volatile long term = -1;
    private volatile String leaderIp = "unknown";

    private volatile long currentBlockHeight;

    public RaftConsensusStateMachine(BlockCommitter committer, BlockSerializer serializer,LedgerRepository ledgerRepository) {
        this.committer = committer;
        this.serializer = serializer;
        this.ledgerRepository = ledgerRepository;
    }

    public void onStart() {
        LedgerBlock ledgerBlock = ledgerRepository.retrieveLatestBlock();
        this.currentBlockHeight = ledgerBlock.getHeight();
    }

    @Override
    public void onApply(Iterator iterator) {
        int index = 0;
        int applied = 0;
        BlockClosure closure = null;
        Block block = null;
        try {
            while (iterator.hasNext()) {
                Status status = Status.OK();
                try {
                    if (iterator.done() != null) {
                        closure = (BlockClosure) iterator.done();
                        block = closure.getBlock();
                    } else {
                        final ByteBuffer data = iterator.getData();
                        block = serializer.deserialize(data.array());
                    }

                    LoggerUtils.debugIfEnabled(LOGGER, "apply state machine log index: {} term: {}, block: {}", iterator.getIndex(), iterator.getTerm(), block);

                    try {
                        boolean result = committer.commitBlock(block, closure);
                        if (result) {
                            currentBlockHeight = block.getHeight();
                        }
                    } catch (BlockCommittedException bce) {
                        //ignore
                        LoggerUtils.debugIfEnabled(LOGGER, "block committed,  ignore it", bce);
                    }

                } catch (Throwable e) {
                    LOGGER.error("apply state machine log index: {} term: {} , txList: {} error", iterator.getIndex(), iterator.getTerm(), block, e);
                    index++;
                    status.setError(RaftError.UNKNOWN, e.toString());
                    throw e;
                }

                applied++;
                index++;
                iterator.next();
            }
        } catch (Throwable t) {
            LOGGER.error("RaftConsensusStateMachine caught error.", t);
            Status status = new Status(RaftError.ESTATEMACHINE, "RaftConsensusStateMachine caught error: %s.", t.getLocalizedMessage());
            iterator.setErrorAndRollback(index - applied, status);
            if (closure != null) {
                closure.run(status);
            }
        }

    }

    @Override
    public void onSnapshotSave(SnapshotWriter writer, Closure done) {
        long height = this.currentBlockHeight;

        Utils.runInThread(() -> {
            final RaftSnapshotFile snapshot = new RaftSnapshotFile(writer.getPath());

            RaftSnapshotFile.RaftSnapshotData snapshotData = new RaftSnapshotFile.RaftSnapshotData();
            snapshotData.setHeight(height);

            if (snapshot.save(snapshotData)) {
                if (writer.addFile(snapshot.getName())) {
                    done.run(Status.OK());
                } else {
                    done.run(new Status(RaftError.EIO, "fail add snap file to writer"));
                }
            } else {
                done.run(new Status(RaftError.EIO, "fail save snapshot %s", snapshot.getFilePath()));
            }
        });
    }


    @Override
    public boolean onSnapshotLoad(SnapshotReader reader) {

        if (isLeader()) {
            LOGGER.warn("leader can not  load snapshot");
            return false;
        }

        RaftSnapshotFile raftSnapshotFile = new RaftSnapshotFile(reader.getPath());

        if (reader.getFileMeta(raftSnapshotFile.getName()) == null) {
            LOGGER.error("fail to find data file in {}", reader.getPath());
            return false;
        }

        try {
            RaftSnapshotFile.RaftSnapshotData load = raftSnapshotFile.load();
            long snapshotHeight = load.getHeight();

            if (snapshotHeight <= this.currentBlockHeight) {
                LOGGER.warn("snapshot height: {} less than current block height: {}, ignore it", snapshotHeight, this.currentBlockHeight);
                return true;
            }

            this.currentBlockHeight = snapshotHeight;
            //TODO: 校验当前账本高度， 若高度不一致，则向leader节点账本请求最新数据


            return true;
        } catch (final IOException e) {
            LOGGER.error("fail to load snapshot from {}", raftSnapshotFile.getFilePath());
            return false;
        }


    }

    @Override
    public void onLeaderStart(final long term) {
        super.onLeaderStart(term);
        this.term = term;
        this.isLeader.set(true);
    }

    @Override
    public void onLeaderStop(final Status status) {
        super.onLeaderStop(status);
        this.isLeader.set(false);
    }

    @Override
    public void onStartFollowing(LeaderChangeContext ctx) {
        this.term = ctx.getTerm();
        this.leaderIp = ctx.getLeaderId().getEndpoint().toString();
    }

    @Override
    public void onError(RaftException e) {
        LOGGER.error("raft consensus state machine caught error", e);
    }

    @Override
    public void onStopFollowing(LeaderChangeContext ctx) {
        super.onStopFollowing(ctx);
        this.term = -1;
        this.isLeader.set(false);
        this.leaderIp = null;
    }

    @Override
    public void onConfigurationCommitted(Configuration conf) {
        super.onConfigurationCommitted(conf);
        //todo publish peers change topic

    }



    public boolean isLeader() {
        return isLeader.get();
    }
//
//    public PeerId getLeader() {
//        if (node == null) {
//            return null;
//        }
//
//        if (node.isLeader()) {
//            return node.getLeaderId();
//        }
//
//        return RouteTable.getInstance().selectLeader(node.getGroupId());
//    }

//    private List<PeerId> allPeers() {
//        if (node == null) {
//            return Collections.emptyList();
//        }
//
//        if (node.isLeader()) {
//            return node.listPeers();
//        }
//
//        return RouteTable.getInstance().getConfiguration(node.getGroupId()).getPeers();
//    }

}
