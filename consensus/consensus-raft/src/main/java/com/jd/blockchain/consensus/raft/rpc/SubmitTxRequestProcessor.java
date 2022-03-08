package com.jd.blockchain.consensus.raft.rpc;

import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;

import java.util.concurrent.Executor;

public class SubmitTxRequestProcessor extends BaseRpcProcessor<SubmitTxRequest> {

    public SubmitTxRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(SubmitTxRequest request, RpcResponseClosure done) {
        getNodeServerService().handleSubmitTxRequest(request, done);
    }

}
