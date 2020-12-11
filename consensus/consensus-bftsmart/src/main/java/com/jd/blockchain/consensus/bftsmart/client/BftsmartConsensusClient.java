package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.MessageService;
import com.jd.blockchain.consensus.bftsmart.BftsmartCredentialInfo;
import com.jd.blockchain.consensus.bftsmart.manage.BftsmartConsensusManageService;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.consensus.manage.ConsensusManageClient;
import com.jd.blockchain.consensus.manage.ConsensusManageService;

public class BftsmartConsensusClient implements ConsensusClient, ConsensusManageClient {

    private BftsmartServiceProxyPool serviceProxyPool;

    private BftsmartClientSettings clientSettings;

    public BftsmartConsensusClient(BftsmartClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    @Override
    public MessageService getMessageService() {
    	return new DynamicBftsmartMessageService();
    }

	@Override
	public ConsensusManageService getManageService() {
		return new DynamicBftsmartConsensusManageService();
	}

    @Override
    public ClientSettings getSettings() {
        return clientSettings;
    }

    @Override
    public boolean isConnected() {
        return this.serviceProxyPool != null;
    }

    @Override
    public synchronized void connect() {
        //consensus client pool
        this.serviceProxyPool = new BftsmartServiceProxyPool(clientSettings);
    }

    @Override
    public void close() {
    	BftsmartServiceProxyPool serviceProxyPool = this.serviceProxyPool;
    	this.serviceProxyPool = null;
        if (serviceProxyPool != null) {
            serviceProxyPool.close();
        }
    }
    
    
    private BftsmartServiceProxyPool ensureConnected() {
    	BftsmartServiceProxyPool serviceProxyPool = this.serviceProxyPool;
    	if (serviceProxyPool == null) {
			throw new IllegalStateException("Client has not conneted to the node servers!");
		}
    	return serviceProxyPool;
    }
    
    private class DynamicBftsmartMessageService extends BftsmartMessageService{

		@Override
		protected BftsmartServiceProxyPool getServiceProxyPool() {
			return ensureConnected();
		}
    	
    }
    
    private class DynamicBftsmartConsensusManageService extends BftsmartConsensusManageService{
    	
    	@Override
    	protected BftsmartServiceProxyPool getServiceProxyPool() {
    		return ensureConnected();
    	}
    	
    }
}
