package com.jd.blockchain.consensus.raft.rpc;

import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;

import java.util.concurrent.Executor;

public class ParticipantNodeRemoveRequestProcessor extends BaseRpcProcessor<ParticipantNodeRemoveRequest> {

    public ParticipantNodeRemoveRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(ParticipantNodeRemoveRequest request, RpcResponseClosure done) {
        getNodeServerService().removeParticipantNode(request, done);
    }

}
