package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.common.primitives.Longs;
import com.jd.blockchain.consensus.raft.consensus.BlockSyncException;
import com.jd.blockchain.consensus.raft.consensus.BlockSyncer;
import com.jd.blockchain.consensus.raft.msgbus.Subcriber;
import com.jd.blockchain.consensus.raft.rpc.QueryManagerInfoRequest;
import com.jd.blockchain.consensus.raft.rpc.QueryManagerInfoRequestProcessor;
import com.jd.blockchain.consensus.raft.rpc.RpcResponse;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.sdk.proxy.HttpBlockchainBrowserService;
import com.jd.blockchain.service.TransactionBatchResultHandle;
import com.jd.httpservice.agent.HttpServiceAgent;
import com.jd.httpservice.agent.ServiceConnection;
import com.jd.httpservice.agent.ServiceConnectionManager;
import com.jd.httpservice.agent.ServiceEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.jd.blockchain.ledger.TransactionState.LEDGER_ERROR;

public class BlockSyncService implements BlockSyncer, Subcriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockSyncService.class);

    private final LedgerRepository repository;
    private final RpcClient rpcClient;
    private final long requestTimeoutMs;

    private volatile boolean isSyncing;

    public BlockSyncService(LedgerRepository repository, RpcClient rpcClient, long requestTimeoutMs) {
        this.repository = repository;
        this.rpcClient = rpcClient;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public void sync(ServiceEndpoint serviceEndpoint, HashDigest ledger, long height) throws BlockSyncException {

        if (!repository.getHash().equals(ledger)) {
            LOGGER.error("sync ledger not match, expect: {} actual: {}", ledger, repository.getHash());
            return;
        }

        try {
            try (ServiceConnection httpConnection = ServiceConnectionManager.connect(serviceEndpoint)) {
                HttpBlockchainBrowserService queryService = HttpServiceAgent.createService(HttpBlockchainBrowserService.class, httpConnection, null);
                LedgerBlock block = queryService.getBlock(ledger, height);
                sync(queryService, ledger, block);
            }
        } catch (Exception e) {
            throw new BlockSyncException(e);
        }
    }

    @Override
    public void sync(Endpoint peerEndpoint, HashDigest ledger, long height) throws BlockSyncException {
        ServiceEndpoint consensusNodeManagerInfo = getConsensusNodeManagerInfo(peerEndpoint);
        if (consensusNodeManagerInfo == null) {
            LoggerUtils.errorIfEnabled(LOGGER, "get peer: {} manager info is null", peerEndpoint);
            throw new BlockSyncException("get peer manager info is null");
        }
        sync(consensusNodeManagerInfo, ledger, height);
    }

    private ServiceEndpoint getConsensusNodeManagerInfo(Endpoint remoteEndpoint) {
        try {
            QueryManagerInfoRequest request = new QueryManagerInfoRequest();
            RpcResponse response = (RpcResponse) rpcClient.invokeSync(remoteEndpoint, request, requestTimeoutMs);

            QueryManagerInfoRequestProcessor.ManagerInfoResponse infoResponse = QueryManagerInfoRequestProcessor.ManagerInfoResponse.fromBytes(response.getResult());
            return new ServiceEndpoint(remoteEndpoint.getIp(), infoResponse.getManagerPort(), infoResponse.isManagerSSLEnabled());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean isSyncing() {
        return isSyncing;
    }

    private void sync(HttpBlockchainBrowserService queryService, HashDigest ledger, LedgerBlock block) {

        if (block == null) {
            throw new IllegalStateException("sync block is null");
        }

        OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
        TransactionBatchProcessor batchProcessor = new TransactionBatchProcessor(repository, opReg);
        List<LedgerTransaction> transactions = getAdditionalTransactions(queryService, ledger, block.getHeight());

        try {
            for (LedgerTransaction ledgerTransaction : transactions) {
                batchProcessor.schedule(ledgerTransaction.getRequest());
            }
        } catch (BlockRollbackException e) {
            batchProcessor.cancel(LEDGER_ERROR);
            throw e;
        }

        LedgerEditor.TIMESTAMP_HOLDER.set(block.getTimestamp());
        TransactionBatchResultHandle handle = batchProcessor.prepare();

        try {
            if (!(handle.getBlock().getHash().toBase58().equals(block.getHash().toBase58()))) {
                throw new IllegalStateException("sync block hash result is inconsistent!");
            }

            if (handle.getBlock().getHeight() != block.getHeight()) {
                throw new IllegalStateException("sync block height is inconsistent!");
            }

            handle.commit();
        } catch (Exception e) {
            handle.cancel(TransactionState.SYSTEM_ERROR);
            throw e;
        }

        LOGGER.debug("sync block at height {}", block.getHeight());
    }

    private List<LedgerTransaction> getAdditionalTransactions(HttpBlockchainBrowserService queryService, HashDigest ledgerHash, long height) {
        List<LedgerTransaction> txs = new ArrayList<>();
        int fromIndex = 0;

        while (true) {
            try {
                LedgerTransactions transactions = queryService.getAdditionalTransactionsInBinary(ledgerHash, height, fromIndex, 100);
                if (null != transactions && null != transactions.getLedgerTransactions()) {
                    LedgerTransaction[] ts = transactions.getLedgerTransactions();
                    fromIndex += ts.length;
                    for (LedgerTransaction tx : ts) {
                        txs.add(tx);
                    }
                    if (ts.length < 100) {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("get transactions from remote error", e);
                throw new IllegalStateException("get transactions from remote error!", e);
            }
        }
        return txs;
    }


    @Override
    public void onMessage(byte[] message) {
        long untilHeight = Longs.fromByteArray(message);
        long latestBlockHeight = repository.retrieveLatestBlockHeight();

        if (latestBlockHeight >= untilHeight) {
            LoggerUtils.debugIfEnabled(LOGGER, "sync height: {} less than current latest height: {}", untilHeight, latestBlockHeight);
            return;
        }

        if (RaftNodeServerContext.getInstance().isLeader(repository.getHash())) {
            LoggerUtils.debugIfEnabled(LOGGER, "current node is leader, can not sync block");
            return;
        }

        PeerId leader = RaftNodeServerContext.getInstance().getLeader(repository.getHash());
        if (leader == null) {
            LoggerUtils.errorIfEnabled(LOGGER, "current leader is null");
            return;
        }

        ServiceEndpoint consensusNodeManagerInfo = getConsensusNodeManagerInfo(leader.getEndpoint());
        LOGGER.debug("get leader: {} manager info: {}", leader, consensusNodeManagerInfo);
        if (consensusNodeManagerInfo == null) {
            LoggerUtils.errorIfEnabled(LOGGER, "get leader: {} manager info is null", leader);
            return;
        }

        isSyncing = true;
        try {
            for (long height = latestBlockHeight + 1; height <= untilHeight; height++) {
                try {
                    sync(consensusNodeManagerInfo, repository.getHash(), height);
                } catch (BlockSyncException e) {
                    LOGGER.error("sync height: {} error", height, e);
                    break;
                }
            }
        } finally {
            isSyncing = false;
        }
    }

    @Override
    public void onQuit() {

    }
}
