package com.jd.blockchain.consensus.raft.server;

import com.google.common.collect.Lists;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerAdminSettings;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.core.*;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;
import utils.codec.Base58Utils;

import java.lang.reflect.Field;

public class BlockProposerServiceTest {

    @Test
    public void testProposalBlock() {

        RaftNodeServer server = new RaftNodeServer(null, null, null);
        BlockProposerService proposer = new BlockProposerService(server, mockLedgerRepository);

        Block b1 = proposer.proposeBlock(Lists.newArrayList(new byte[][]{}));

        Block b2 = proposer.proposeBlock(Lists.newArrayList(new byte[][]{}));
        Block b3 = proposer.proposeBlock(Lists.newArrayList(new byte[][]{}));
        Block b4 = proposer.proposeBlock(Lists.newArrayList(new byte[][]{}));
        Block b5 = proposer.proposeBlock(Lists.newArrayList(new byte[][]{}));
        Block b6 = proposer.proposeBlock(Lists.newArrayList(new byte[][]{}));

        b1.setCurrentBlockHash("b1");
        proposer.commitCallBack(b1, true);

        b2.setCurrentBlockHash("b2");
        proposer.commitCallBack(b2, true);

        b3.setCurrentBlockHash("b3");
        proposer.commitCallBack(b3, false);

        b4.setCurrentBlockHash("b4");
        proposer.commitCallBack(b4, true);

        b5.setCurrentBlockHash("b5");
        proposer.commitCallBack(b5, false);

        b6.setCurrentBlockHash("b6");
        proposer.commitCallBack(b6, true);


    }


    LedgerRepository mockLedgerRepository = new LedgerRepository() {
        @Override
        public LedgerEditor createNextBlock() {
            return null;
        }

        @Override
        public LedgerEditor getNextBlockEditor() {
            return null;
        }

        @Override
        public LedgerSecurityManager getSecurityManager() {
            return null;
        }

        @Override
        public LedgerDiffView getDiffView(LedgerBlock recentBlock, LedgerBlock previousBlock) {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public HashDigest getHash() {
            return null;
        }

        @Override
        public long getVersion() {
            return 0;
        }

        @Override
        public long getLatestBlockHeight() {
            return 0;
        }

        @Override
        public HashDigest getLatestBlockHash() {
            return null;
        }

        @Override
        public LedgerBlock getLatestBlock() {
            return null;
        }

        @Override
        public HashDigest getBlockHash(long height) {
            return null;
        }

        @Override
        public LedgerDataStructure getLedgerDataStructure() {
            return null;
        }

        @Override
        public LedgerBlock getBlock(long height) {
            return null;
        }

        @Override
        public LedgerAdminInfo getAdminInfo() {
            return null;
        }

        @Override
        public LedgerAdminInfo getAdminInfo(LedgerBlock block) {
            return null;
        }

        @Override
        public LedgerAdminSettings getAdminSettings() {
            return null;
        }

        @Override
        public LedgerAdminSettings getAdminSettings(LedgerBlock block) {
            return null;
        }

        @Override
        public LedgerBlock getBlock(HashDigest hash) {
            return null;
        }

        @Override
        public LedgerDataSet getLedgerDataSet(LedgerBlock block) {
            return null;
        }

        @Override
        public LedgerEventSet getLedgerEventSet(LedgerBlock block) {
            return null;
        }

        @Override
        public TransactionSet getTransactionSet(LedgerBlock block) {
            return null;
        }

        @Override
        public UserAccountSet getUserAccountSet(LedgerBlock block) {
            return null;
        }

        @Override
        public DataAccountSet getDataAccountSet(LedgerBlock block) {
            return null;
        }

        @Override
        public ContractAccountSet getContractAccountSet(LedgerBlock block) {
            return null;
        }

        @Override
        public EventGroup getSystemEventGroup(LedgerBlock block) {
            return null;
        }

        @Override
        public EventAccountSet getEventAccountSet(LedgerBlock block) {
            return null;
        }

        @Override
        public LedgerBlock retrieveLatestBlock() {

            LedgerBlockData ledgerBlock = new LedgerBlockData();
            ledgerBlock.setHash(Crypto.resolveAsHashDigest(Base58Utils.decode("j5j4L7EcFkVJUyLN4wWbT66pTwsgNSsnHYwC3xSducjer6")));
            try {
                Field height = ledgerBlock.getClass().getDeclaredField("height");
                ReflectionUtils.makeAccessible(height);
                ReflectionUtils.setField(height, ledgerBlock, 10);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }


            return ledgerBlock;
        }

        @Override
        public long retrieveLatestBlockHeight() {
            return 0;
        }

        @Override
        public HashDigest retrieveLatestBlockHash() {
            return null;
        }
    };


}
