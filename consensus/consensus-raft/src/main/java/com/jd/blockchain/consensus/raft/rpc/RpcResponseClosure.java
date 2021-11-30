package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;

public abstract class RpcResponseClosure implements Closure {

    private Object request;

    private RpcResponse response;

    public RpcResponseClosure(Object request) {
        this.request = request;
    }

    public RpcResponse getResponse(Status status) {
        if(response != null){
            return response;
        }

        if(Status.OK().equals(status)){
            return RpcResponse.success(null);
        }

        if(status != null){
            return RpcResponse.fail(status.getCode(), status.getErrorMsg());
        }

        return RpcResponse.success(null);
    }

    public void setResponse(RpcResponse response) {
        this.response = response;
    }

    public Object getRequest() {
        return request;
    }

}
