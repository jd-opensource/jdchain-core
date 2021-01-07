package com.jd.blockchain.consensus.bftsmart.binaryproto;

import com.jd.blockchain.binaryproto.DataContractAutoRegistrar;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.configure.ConsensusDataContractAutoRegistrar;

public class BftsmartDataContractAutoRegistrar implements DataContractAutoRegistrar{
	
	@Override
	public int order() {
		return ConsensusDataContractAutoRegistrar.ORDER + 1;
	}

	@Override
	public void initContext(DataContractRegistry registry) {
		DataContractRegistry.register(BftsmartConsensusViewSettings.class);
		DataContractRegistry.register(BftsmartNodeSettings.class);
		DataContractRegistry.register(BftsmartClientIncomingSettings.class);
		DataContractRegistry.register(BftsmartSessionCredential.class);
	}

}
