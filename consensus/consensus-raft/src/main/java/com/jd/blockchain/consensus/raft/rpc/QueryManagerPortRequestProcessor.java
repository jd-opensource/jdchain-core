package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Status;
import com.google.common.primitives.Ints;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;
import com.jd.blockchain.runtime.RuntimeConstant;

import java.util.concurrent.Executor;

public class QueryManagerPortRequestProcessor extends BaseRpcProcessor<QueryManagerPortRequest> {

    public QueryManagerPortRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(QueryManagerPortRequest request, RpcResponseClosure done) {
        done.setResponse(RpcResponse.success(Ints.toByteArray(RuntimeConstant.getMonitorPort())));
        done.run(Status.OK());
    }


}
