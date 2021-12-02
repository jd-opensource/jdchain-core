package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.PeerId;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;
import com.jd.blockchain.ledger.TransactionState;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public class QueryLeaderRequestProcessor extends BaseRpcProcessor<QueryLeaderRequest> {

    public QueryLeaderRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(QueryLeaderRequest request, RpcResponseClosure done) {
        PeerId leader = getNodeServerService().getNodeServer().getLeader();
        if (leader == null) {
            done.run(new Status(TransactionState.PARTICIPANT_DOES_NOT_EXIST.CODE, "leader not exist"));
            return;
        }

        done.setResponse(RpcResponse.success(leader.getEndpoint().toString().getBytes(StandardCharsets.UTF_8)));
        done.run(Status.OK());
    }
}
