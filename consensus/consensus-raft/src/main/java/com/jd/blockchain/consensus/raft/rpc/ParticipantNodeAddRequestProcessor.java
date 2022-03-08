package com.jd.blockchain.consensus.raft.rpc;

import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;

import java.util.concurrent.Executor;

public class ParticipantNodeAddRequestProcessor extends BaseRpcProcessor<ParticipantNodeAddRequest> {


    public ParticipantNodeAddRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }


    @Override
    protected void processRequest(ParticipantNodeAddRequest request, RpcResponseClosure done) {
        getNodeServerService().addParticipantNode(request, done);
    }

}
