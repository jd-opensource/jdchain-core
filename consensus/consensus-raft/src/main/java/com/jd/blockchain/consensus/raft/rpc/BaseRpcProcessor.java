package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;
import com.jd.blockchain.consensus.raft.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.Executor;

public abstract class BaseRpcProcessor<T> implements RpcProcessor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitTxRequestProcessor.class);

    private RaftNodeServerService nodeServerService;
    private Executor executor;

    public BaseRpcProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        this.nodeServerService = nodeServerService;
        this.executor = executor;
    }

    @Override
    public void handleRequest(RpcContext rpcCtx, T request) {
        LoggerUtils.debugIfEnabled(LOGGER, "receive tx request: {}", request);

        final RpcResponseClosure done = new RpcResponseClosure(request) {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getResponse(status));
            }
        };

        processRequest(request, done);
    }

    protected abstract void processRequest(T request, RpcResponseClosure done);

    @Override
    public String interest() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getName();
    }

    @Override
    public Executor executor() {
        return executor;
    }

    public RaftNodeServerService getNodeServerService() {
        return nodeServerService;
    }

}
