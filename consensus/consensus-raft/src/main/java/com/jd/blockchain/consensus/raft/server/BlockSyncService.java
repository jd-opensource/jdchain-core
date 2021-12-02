package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.common.primitives.Ints;
import com.jd.blockchain.consensus.raft.consensus.BlockSyncException;
import com.jd.blockchain.consensus.raft.consensus.BlockSyncer;
import com.jd.blockchain.consensus.raft.rpc.QueryManagerPortRequest;
import com.jd.blockchain.consensus.raft.rpc.RpcResponse;
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

public class BlockSyncService implements BlockSyncer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockSyncService.class);

    private HashDigest ledger;
    private LedgerRepository repository;
    private RpcClient rpcClient;
    private long requestTimeoutMs;

    public BlockSyncService(HashDigest ledger, LedgerRepository repository, RpcClient rpcClient, long requestTimeoutMs) {
        this.ledger = ledger;
        this.repository = repository;
        this.rpcClient = rpcClient;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public boolean sync(String consensusHost, int consensusPort, long height) throws BlockSyncException {
        boolean syncResult = true;
        try {
            QueryManagerPortRequest request = new QueryManagerPortRequest();
            RpcResponse response = (RpcResponse) rpcClient.invokeSync(new Endpoint(consensusHost, consensusPort), request, requestTimeoutMs);

            if (!response.isSuccess()) {
                return false;
            }

            int managerPort = Ints.fromByteArray(response.getResult());
            ServiceEndpoint remoteEndpoint = new ServiceEndpoint(consensusHost, managerPort, false); //todo

            try (ServiceConnection httpConnection = ServiceConnectionManager.connect(remoteEndpoint)) {
                HttpBlockchainBrowserService queryService = HttpServiceAgent.createService(HttpBlockchainBrowserService.class, httpConnection, null);
                LedgerBlock block = queryService.getBlock(ledger, height);
                sync(queryService, block);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new BlockSyncException(e);
        }

        return syncResult;
    }

    private void sync(HttpBlockchainBrowserService queryService, LedgerBlock block) {
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

        if (!(handle.getBlock().getHash().toBase58().equals(block.getHash().toBase58()))) {
            throw new IllegalStateException("sync block hash result is inconsistent!");
        }
        handle.commit();
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
    public ParticipantNode findParticipantNode() {
        return null;
    }

    @Override
    public boolean sync(ParticipantNode node, long height) throws BlockSyncException {
        return false;
    }
}
