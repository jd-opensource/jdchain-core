package com.jd.blockchain.consensus.bftsmart.manage;

import com.jd.blockchain.consensus.bftsmart.client.BftsmartClientSettings;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartConsensusClient;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartConsensusClientFactory;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.manage.ConsensusManageClient;
import com.jd.blockchain.consensus.manage.ManageClientFactory;

public class BftsmartConsensusManageClientFactory extends BftsmartConsensusClientFactory implements ManageClientFactory {

	@Override
	public ConsensusManageClient setupManageClient(ClientSettings settings) {
		return new BftsmartConsensusClient((BftsmartClientSettings) settings);
	}



}
