package com.jd.blockchain.consensus.bftsmart.client;

import bftsmart.tom.AsynchServiceProxy;
import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.utils.concurrent.AsyncFuture;
import com.jd.blockchain.utils.concurrent.CompletableAsyncFuture;
import com.jd.blockchain.utils.io.BytesUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BftsmartMessageService implements MessageService {

    private BftsmartServiceProxyPool serviceProxyPool;

    public BftsmartMessageService(BftsmartServiceProxyPool peerProxyPool) {
        this.serviceProxyPool = peerProxyPool;
    }

    @Override
    public AsyncFuture<byte[]> sendOrdered(byte[] message) {
        return sendOrderedMessage(message);
    }

    private AsyncFuture<byte[]> sendOrderedMessage(byte[] message) {
        CompletableAsyncFuture<byte[]> asyncFuture = new CompletableAsyncFuture<>();
        AsynchServiceProxy asynchServiceProxy = null;
        try {
            asynchServiceProxy = serviceProxyPool.borrowObject();
            
            //TODO: 此处是同步调用，今后应优化为异步调用；
            byte[] result = asynchServiceProxy.invokeOrdered(message);
            
            asyncFuture.complete(result);
        } catch (Exception e) {
        	asyncFuture.error(e);
//            throw new RuntimeException(e);
        } finally {
            if (asynchServiceProxy != null) {
                serviceProxyPool.returnObject(asynchServiceProxy);
            }
        }

        return asyncFuture;
    }

    @Override
    public AsyncFuture<byte[]> sendUnordered(byte[] message) {
        return sendUnorderedMessage(message);
    }

    private AsyncFuture<byte[]> sendUnorderedMessage(byte[] message) {
        CompletableAsyncFuture<byte[]> asyncFuture = new CompletableAsyncFuture<>();
            AsynchServiceProxy asynchServiceProxy = null;
            try {
                asynchServiceProxy = serviceProxyPool.borrowObject();
                byte[] result = asynchServiceProxy.invokeUnordered(message);
                asyncFuture.complete(result);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (asynchServiceProxy != null) {
                    serviceProxyPool.returnObject(asynchServiceProxy);
                }
            }
        return asyncFuture;
    }
}
