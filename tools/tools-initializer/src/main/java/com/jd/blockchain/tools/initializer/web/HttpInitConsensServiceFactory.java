package com.jd.blockchain.tools.initializer.web;

import com.jd.httpservice.agent.HttpServiceAgent;
import com.jd.httpservice.agent.ServiceEndpoint;

import com.jd.httpservice.auth.SSLSecurity;
import utils.net.NetworkAddress;

public class HttpInitConsensServiceFactory implements InitConsensusServiceFactory {

	@Override
	public LedgerInitConsensusService connect(NetworkAddress endpointAddress) {
		ServiceEndpoint endpoint = new ServiceEndpoint(endpointAddress);
		endpoint.setSslSecurity(new SSLSecurity());
		LedgerInitConsensusService initConsensus = HttpServiceAgent.createService(LedgerInitConsensusService.class,
				endpoint);
		return initConsensus;
	}

}
