package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubmitTxRequestProcessor implements RpcProcessor<SubmitTxRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitTxRequestProcessor.class);

    private RaftNodeServerService nodeServerService;

    public SubmitTxRequestProcessor(RaftNodeServerService nodeServerService) {
        this.nodeServerService = nodeServerService;
    }

    @Override
    public void handleRequest(RpcContext rpcContext, SubmitTxRequest submitTxRequest) {

        LoggerUtils.debugIfEnabled(LOGGER, "node: {} receive tx request: {}", nodeServerService.getNodeServer().getNode().getNodeId(), submitTxRequest);

        final Closure done = new RpcResponseClosure(submitTxRequest) {
            @Override
            public void run(Status status) {
                rpcContext.sendResponse(getResponse(status));
            }
        };

        nodeServerService.handleSubmitTxRequest(submitTxRequest, done);
    }

    @Override
    public String interest() {
        return SubmitTxRequest.class.getName();
    }
}
