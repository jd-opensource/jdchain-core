package com.jd.blockchain.consensus.raft.server;

import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockCommitCallback;
import com.jd.blockchain.consensus.raft.consensus.BlockProposer;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.core.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class BlockProposerService implements BlockProposer, BlockCommitCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockProposerService.class);

    private TreeMap<Long, Block> proposalBlockMap = new TreeMap<>();
    private Block latestProposalBlock;

    private LedgerRepository ledgerRepository;

    private static final ReentrantLock lock = new ReentrantLock();

    public BlockProposerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public Block proposeBlock(List<byte[]> txs) {
        lock.lock();
        Block proposeBlock = new Block();
        try {
            proposeBlock.setProposalTimestamp(System.currentTimeMillis());
            proposeBlock.setTxs(txs);
            //todo: 使用latestProposalBlock减少账本查询
            if (proposalBlockMap.isEmpty()) {
                LedgerBlock latestBlock = ledgerRepository.retrieveLatestBlock();
                proposeBlock.setPreBlockHash(latestBlock.getHash());
                proposeBlock.setHeight(latestBlock.getHeight() + 1);
            } else {
                proposeBlock.setHeight(latestProposalBlock.getHeight() + +1);
            }

            proposalBlockMap.put(proposeBlock.getHeight(), proposeBlock);
            latestProposalBlock = proposeBlock;

            LoggerUtils.debugIfEnabled(LOGGER, "proposal cache size: {}, latest proposal height: {}", proposalBlockMap.size(), latestProposalBlock.getHeight());
        } finally {
            lock.unlock();
        }
        return proposeBlock;
    }

    @Override
    public boolean canPropose() {
        return RaftNodeServerContext.getInstance().isLeader(this.ledgerRepository.getHash());
    }

    @Override
    public void clear() {
        proposalBlockMap.clear();
        latestProposalBlock = null;
    }

    @Override
    public void commitCallBack(Block block, boolean isCommit) {
        lock.lock();
        try {
            proposalBlockMap.headMap(block.getHeight(), true).clear();
            long nextHeight = block.getHeight() + 1;
            Block blockCache = proposalBlockMap.get(nextHeight);

            if(isCommit){
                Optional.ofNullable(blockCache).ifPresent(b -> b.setPreBlockHash(block.getCurrentBlockHash()));
            }else{
                Optional.ofNullable(blockCache).ifPresent(b -> b.setPreBlockHash(block.getPreBlockHash()));

                TreeMap<Long, Block> blockTreeMap = new TreeMap<>();
                proposalBlockMap.values().forEach(b -> {
                    b.setHeight(b.getHeight() - 1);
                    blockTreeMap.put(b.getHeight(), b);
                });

                proposalBlockMap.clear();
                proposalBlockMap = null;
                proposalBlockMap = blockTreeMap;
            }
        } finally {
            lock.unlock();
        }
    }

}
