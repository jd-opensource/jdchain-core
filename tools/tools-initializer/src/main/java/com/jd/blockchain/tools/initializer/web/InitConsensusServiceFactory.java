package com.jd.blockchain.tools.initializer.web;

import utils.net.NetworkAddress;

public interface InitConsensusServiceFactory {
	
	public LedgerInitConsensusService connect(NetworkAddress endpointAddress);
	
}
