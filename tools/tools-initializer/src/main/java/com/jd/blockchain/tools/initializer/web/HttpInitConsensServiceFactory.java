package com.jd.blockchain.tools.initializer.web;

import com.jd.blockchain.utils.net.NetworkAddress;
import com.jd.httpservice.agent.HttpServiceAgent;
import com.jd.httpservice.agent.ServiceEndpoint;

public class HttpInitConsensServiceFactory implements InitConsensusServiceFactory {

	@Override
	public LedgerInitConsensusService connect(NetworkAddress endpointAddress) {
		ServiceEndpoint endpoint = new ServiceEndpoint(endpointAddress);
		LedgerInitConsensusService initConsensus = HttpServiceAgent.createService(LedgerInitConsensusService.class,
				endpoint);
		return initConsensus;
	}

}
