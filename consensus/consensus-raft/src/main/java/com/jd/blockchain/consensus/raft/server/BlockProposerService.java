package com.jd.blockchain.consensus.raft.server;

import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.consensus.BlockCommitCallback;
import com.jd.blockchain.consensus.raft.consensus.BlockProposer;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.core.LedgerRepository;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class BlockProposerService implements BlockProposer, BlockCommitCallback {

    private TreeMap<Long, Block> proposalBlockMap = new TreeMap<>();

    private Block latestProposalBlock;

    private RaftNodeServer nodeServer;

    private LedgerRepository ledgerRepository;

    private static final ReentrantLock lock = new ReentrantLock();

    public BlockProposerService(RaftNodeServer nodeServer, LedgerRepository ledgerRepository) {
        this.nodeServer = nodeServer;
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
                proposeBlock.setPreBlockHash(latestBlock.getHash().toBase58());
                proposeBlock.setHeight(latestBlock.getHeight() + 1);
            } else {
                proposeBlock.setHeight(latestProposalBlock.getHeight() + +1);
            }

            proposalBlockMap.put(proposeBlock.getHeight(), proposeBlock);
            latestProposalBlock = proposeBlock;

        } finally {
            lock.unlock();
        }
        return proposeBlock;
    }

    @Override
    public boolean canPropose() {
        return nodeServer.isLeader();
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
