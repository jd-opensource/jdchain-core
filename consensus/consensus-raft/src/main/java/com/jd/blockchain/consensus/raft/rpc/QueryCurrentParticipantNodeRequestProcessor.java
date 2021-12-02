package com.jd.blockchain.consensus.raft.rpc;

import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;
import com.jd.blockchain.ledger.core.LedgerRepository;
import com.jd.blockchain.peer.spring.LedgerManageUtils;

import java.util.concurrent.Executor;

public class QueryCurrentParticipantNodeRequestProcessor extends BaseRpcProcessor<QueryCurrentParticipantNodeRequest>{


    public QueryCurrentParticipantNodeRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(QueryCurrentParticipantNodeRequest request, RpcResponseClosure done) {

        LedgerRepository ledgerRepository = LedgerManageUtils.getLedgerRepository(request.getLedgerHash());



    }
}
