package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;

public class ParticipantNodeChangeRequestProcessor implements RpcProcessor<ParticipantNodeChangeRequest> {

    private RaftNodeServerService nodeServerService;

    public ParticipantNodeChangeRequestProcessor(RaftNodeServerService nodeServerService) {
        this.nodeServerService = nodeServerService;
    }

    @Override
    public void handleRequest(RpcContext rpcContext, ParticipantNodeChangeRequest participantNodeChangeRequest) {
        final Closure done = new RpcResponseClosure(participantNodeChangeRequest) {
            @Override
            public void run(Status status) {
                rpcContext.sendResponse(getResponse(status));
            }
        };

        nodeServerService.handleParticipantNodeChangeRequest(participantNodeChangeRequest, done);
    }

    @Override
    public String interest() {
        return ParticipantNodeChangeRequest.class.getName();
    }
}
