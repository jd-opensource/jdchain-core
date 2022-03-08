package com.jd.blockchain.consensus.raft.rpc;

import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;

import java.util.concurrent.Executor;

public class ParticipantNodeTransferRequestProcessor extends BaseRpcProcessor<ParticipantNodeTransferRequest> {

    public ParticipantNodeTransferRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(ParticipantNodeTransferRequest request, RpcResponseClosure done) {
        getNodeServerService().transferParticipantNode(request, done);
    }

}
