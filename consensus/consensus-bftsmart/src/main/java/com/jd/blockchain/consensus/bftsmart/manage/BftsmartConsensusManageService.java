package com.jd.blockchain.consensus.bftsmart.manage;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.bftsmart.BftsmartReplica;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartServiceProxyPool;
import com.jd.blockchain.consensus.manage.ConsensusManageService;
import com.jd.blockchain.consensus.manage.ConsensusView;

import bftsmart.reconfiguration.Reconfiguration;
import bftsmart.reconfiguration.ReconfigureReply;
import bftsmart.tom.AsynchServiceProxy;
import utils.concurrent.AsyncFuture;
import utils.concurrent.CompletableAsyncFuture;

public abstract class BftsmartConsensusManageService implements ConsensusManageService {

	public BftsmartConsensusManageService() {
	}

	protected abstract BftsmartServiceProxyPool getServiceProxyPool();

	@Override
	public AsyncFuture<ConsensusView> addNode(Replica replica) {
		BftsmartServiceProxyPool serviceProxyPool = getServiceProxyPool();
		
		BftsmartReplica bftsmartReplica = (BftsmartReplica) replica;

		CompletableAsyncFuture<ConsensusView> asyncFuture = new CompletableAsyncFuture<>();
		AsynchServiceProxy serviceProxy = null;
		try {
			serviceProxy = serviceProxyPool.borrowObject();

			Reconfiguration reconfiguration = new Reconfiguration(serviceProxy.getProcessId(), serviceProxy);

			reconfiguration.addServer(bftsmartReplica.getId(), bftsmartReplica.getNetworkAddress().getHost(),
					bftsmartReplica.getNetworkAddress().getPort());

			ReconfigureReply reply = reconfiguration.execute();

			asyncFuture.complete(new BftsmartView(reply.getView()));
		} catch (Exception e) {
			asyncFuture.error(e);
		} finally {
			if (serviceProxy != null) {
				serviceProxyPool.returnObject(serviceProxy);
			}
		}

		return asyncFuture;

	}

	@Override
	public AsyncFuture<ConsensusView> removeNode(Replica replica) {
		BftsmartServiceProxyPool serviceProxyPool = getServiceProxyPool();
		
		CompletableAsyncFuture<ConsensusView> asyncFuture = new CompletableAsyncFuture<>();
		AsynchServiceProxy serviceProxy = null;
		try {
			serviceProxy = serviceProxyPool.borrowObject();

			Reconfiguration reconfiguration = new Reconfiguration(serviceProxy.getProcessId(), serviceProxy);

			reconfiguration.removeServer(replica.getId());

			ReconfigureReply reply = reconfiguration.execute();

			asyncFuture.complete(new BftsmartView(reply.getView()));
		} catch (Exception e) {
			asyncFuture.error(e);
		} finally {
			if (serviceProxy != null) {
				serviceProxyPool.returnObject(serviceProxy);
			}
		}

		return asyncFuture;
	}

}
