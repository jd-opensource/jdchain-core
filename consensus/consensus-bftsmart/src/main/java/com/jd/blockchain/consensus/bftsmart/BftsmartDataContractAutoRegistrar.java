package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.binaryproto.DataContractAutoRegistrar;
import com.jd.blockchain.binaryproto.DataContractRegistry;

public class BftsmartDataContractAutoRegistrar implements DataContractAutoRegistrar{

	@Override
	public void initContext(DataContractRegistry registry) {
		DataContractRegistry.register(BftsmartConsensusSettings.class);
		DataContractRegistry.register(BftsmartNodeSettings.class);
		
	}

}
