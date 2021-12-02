package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Status;
import com.jd.blockchain.consensus.raft.consensus.Block;

import com.jd.blockchain.consensus.raft.consensus.BlockCommitCallback;
import com.jd.blockchain.consensus.raft.consensus.BlockCommittedException;
import com.jd.blockchain.consensus.raft.consensus.BlockCommitter;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.core.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.concurrent.AsyncFuture;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;

public class BlockCommitService implements BlockCommitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockCommitService.class);

    private String realmName;
    private MessageHandle messageHandle;
    private LedgerRepository ledgerRepository;
    private List<BlockCommitCallback> blockCommitCallbackList = new ArrayList<>();

    public BlockCommitService(String realmName, MessageHandle messageHandle, LedgerRepository ledgerRepository) {
        this.realmName = realmName;
        this.messageHandle = messageHandle;
        this.ledgerRepository = ledgerRepository;
    }

    public boolean commitBlock(Block block, BlockClosure done) throws BlockCommittedException {

        boolean result = true;

        LedgerBlock latestBlock = ledgerRepository.retrieveLatestBlock();
        if (latestBlock.getHeight() >= block.getHeight()) {
            throw new BlockCommittedException(block.getHeight());
        }

        if (latestBlock.getHeight() + 1 != block.getHeight()) {
            LOGGER.error("commit block ignore. expect height:{}, latest block: {}", block.getHeight(), latestBlock.getHeight());
            return false;
        }

        RaftConsensusMessageContext context = RaftConsensusMessageContext.createContext(realmName);
        context.setTimestamp(block.getProposalTimestamp());

        String batch = messageHandle.beginBatch(context);
        context.setBatchId(batch);

        LoggerUtils.debugIfEnabled(LOGGER, "commit block start, batchId: {}", batch);

        Status status = Status.OK();
        try {
            int msgId = 0;
            for (byte[] tx : block.getTxs()) {
                AsyncFuture<byte[]> asyncFuture = messageHandle.processOrdered(msgId++, tx, context);
                Optional.ofNullable(done).ifPresent(d -> d.addFuture(asyncFuture));
            }

            messageHandle.completeBatch(context);
            //todo ?
            messageHandle.commitBatch(context);

            LedgerBlock repositoryLatestBlock = ledgerRepository.getLatestBlock();
            assert repositoryLatestBlock.getHeight() == block.getHeight();

            block.setPreBlockHash(repositoryLatestBlock.getPreviousHash().toBase58());
            block.setCurrentBlockHash(repositoryLatestBlock.getHash().toBase58());

            blockCommitCallbackList.forEach(c -> c.commitCallBack(block, true));

        } catch (Exception e) {
            LOGGER.error("commitBlock error", e);
            result = false;
            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, context);
            status = new Status(TransactionState.CONSENSUS_ERROR.CODE, e.getMessage());
            blockCommitCallbackList.forEach(c -> c.commitCallBack(block, false));
        }

        LoggerUtils.debugIfEnabled(LOGGER, "commit block end, batchId: {}, blockHeight: {}, status: {}", batch, block.getHeight(), status);

        if (done != null) {
            done.run(status);
        }

        return result;
    }

    @Override
    public synchronized void registerCallBack(BlockCommitCallback callback) {
        if (this.blockCommitCallbackList.contains(callback)) {
            return;
        }
        this.blockCommitCallbackList.add(callback);
    }

}
