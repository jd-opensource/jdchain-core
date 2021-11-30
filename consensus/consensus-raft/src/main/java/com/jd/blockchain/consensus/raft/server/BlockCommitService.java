package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Status;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockCommittedException;
import com.jd.blockchain.consensus.raft.consensus.BlockCommitter;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.core.LedgerManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.codec.Base58Utils;
import utils.concurrent.AsyncFuture;

import java.util.Optional;

public class BlockCommitService implements BlockCommitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockCommitService.class);

    private String realmName;
    private HashDigest ledgerHash;
    private MessageHandle messageHandle;
    private LedgerManager ledgerManager;

    public BlockCommitService(String realmName, MessageHandle messageHandle, LedgerManager ledgerManager) {
        this.realmName = realmName;
        this.ledgerHash = Crypto.resolveAsHashDigest(Base58Utils.decode(realmName));
        this.messageHandle = messageHandle;
        this.ledgerManager = ledgerManager;
    }

    public boolean commitBlock(Block block, BlockClosure done) throws BlockCommittedException {

        boolean result = true;

        LedgerBlock latestBlock = ledgerManager.getLedger(ledgerHash).retrieveLatestBlock();
        if(latestBlock.getHeight() >= block.getHeight()){
            throw new BlockCommittedException(block.getHeight());
        }

        if(latestBlock.getHeight() + 1 != block.getHeight()){
            LOGGER.error("commit block ignore. expect height:{}, latest block: {}", block.getHeight(), latestBlock.getHeight());
            return false;
        }

        RaftConsensusMessageContext context = RaftConsensusMessageContext.createContext(realmName);
        String batch = messageHandle.beginBatch(context);
        context.setBatchId(batch);

        LoggerUtils.debugIfEnabled(LOGGER, "commit block start, batchId: {}", batch);

        Status status = null;
        Long blockHeight = null;

        try{
            int msgId = 0;
            for(byte[] tx : block.getTxs()){
                AsyncFuture<byte[]> asyncFuture = messageHandle.processOrdered(msgId++, tx, context);
                Optional.ofNullable(done).ifPresent(d -> d.addFuture(asyncFuture));
            }
            StateSnapshot stateSnapshot = messageHandle.completeBatch(context);
            blockHeight = stateSnapshot.getId();

            assert blockHeight == block.getHeight();

            messageHandle.commitBatch(context);
            status = Status.OK();
        }catch (Exception e){
            LOGGER.error("commitBlock error", e);
            result = false;
            messageHandle.rollbackBatch(TransactionState.CONSENSUS_ERROR.CODE, context);
            status = new Status(TransactionState.CONSENSUS_ERROR.CODE, e.getMessage());
        }

        LoggerUtils.debugIfEnabled(LOGGER, "commit block end, batchId: {}, blockHeight: {}, status: {}", batch, blockHeight, status);

        if(done != null){
            done.run(status);
        }

        return result;
    }

}
