package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.util.Utils;
import com.jd.blockchain.consensus.raft.consensus.Block;
import com.jd.blockchain.consensus.raft.rpc.RpcResponse;
import com.jd.blockchain.consensus.raft.rpc.RpcResponseClosure;
import utils.concurrent.AsyncFuture;

import java.util.ArrayList;
import java.util.List;

public class BlockClosure implements Closure {

    private List<AsyncFuture<byte[]>> txResultList;

    private Block block;

    private List<Closure> doneList;

    public BlockClosure(Block block, List<Closure> doneList) {
        this.block = block;
        this.doneList = doneList;
        this.txResultList = new ArrayList<>(block.getTxs().size());
    }

    @Override
    public void run(Status status) {
        int i = 0;
        for (Closure closure : this.getDoneList()) {
            int index = i;
            RpcResponseClosure submitTxDone = (RpcResponseClosure) closure;
            if (submitTxDone != null) {
                Utils.runClosureInThread(s -> {
                    if (status.isOk()) {
                        AsyncFuture<byte[]> future = getFuture(index);
                        submitTxDone.setResponse(RpcResponse.success(future.get()));
                    } else {
                        submitTxDone.setResponse(RpcResponse.fail(status.getCode(), status.getErrorMsg()));
                    }
                    submitTxDone.run(s);
                });
            }
            i++;
        }
    }

    public void addFuture(AsyncFuture<byte[]> future) {
        txResultList.add(future);
    }

    public AsyncFuture<byte[]> getFuture(int index) {
        if (index >= txResultList.size() || index < 0) {
            return null;
        }
        return txResultList.get(index);
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public List<Closure> getDoneList() {
        return doneList;
    }
}
