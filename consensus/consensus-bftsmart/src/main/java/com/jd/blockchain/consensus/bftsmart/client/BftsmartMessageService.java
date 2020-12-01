package com.jd.blockchain.consensus.bftsmart.client;

import bftsmart.tom.AsynchServiceProxy;
import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.utils.concurrent.AsyncFuture;
import com.jd.blockchain.utils.concurrent.CompletableAsyncFuture;
import com.jd.blockchain.utils.exception.ViewObsoleteException;

public abstract class BftsmartMessageService implements MessageService {

//    private BftsmartServiceProxyPool asyncPeerProxyPool;

	public BftsmartMessageService() {
	}

	protected abstract BftsmartServiceProxyPool getServiceProxyPool();

	@Override
	public AsyncFuture<byte[]> sendOrdered(byte[] message) {
		return sendOrderedMessage(message);
	}

	private AsyncFuture<byte[]> sendOrderedMessage(byte[] message) {
		BftsmartServiceProxyPool asyncPeerProxyPool = getServiceProxyPool();
		
		CompletableAsyncFuture<byte[]> asyncFuture = new CompletableAsyncFuture<>();
		AsynchServiceProxy asynchServiceProxy = null;
		try {
			asynchServiceProxy = asyncPeerProxyPool.borrowObject();
			
			byte[] result = asynchServiceProxy.invokeOrdered(message);
			asyncFuture.complete(result);
		} catch (ViewObsoleteException voe) {
			throw voe;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (asynchServiceProxy != null) {
				asyncPeerProxyPool.returnObject(asynchServiceProxy);
			}
		}

		return asyncFuture;
	}

	@Override
	public AsyncFuture<byte[]> sendUnordered(byte[] message) {
		return sendUnorderedMessage(message);
	}

	private AsyncFuture<byte[]> sendUnorderedMessage(byte[] message) {
		BftsmartServiceProxyPool asyncPeerProxyPool = getServiceProxyPool();
		
		CompletableAsyncFuture<byte[]> asyncFuture = new CompletableAsyncFuture<>();
		AsynchServiceProxy asynchServiceProxy = null;
		try {
			asynchServiceProxy = asyncPeerProxyPool.borrowObject();
			byte[] result = asynchServiceProxy.invokeUnordered(message);
			asyncFuture.complete(result);

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (asynchServiceProxy != null) {
				asyncPeerProxyPool.returnObject(asynchServiceProxy);
			}
		}
		return asyncFuture;
	}
}
