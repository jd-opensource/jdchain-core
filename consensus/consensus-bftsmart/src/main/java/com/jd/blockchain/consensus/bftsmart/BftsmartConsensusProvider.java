package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.ledger.ConsensusTypeEnum;
import com.jd.blockchain.consensus.SettingsFactory;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartConsensusClientFactory;
import com.jd.blockchain.consensus.bftsmart.manage.BftsmartConsensusManageClientFactory;
import com.jd.blockchain.consensus.bftsmart.service.BftsmartNodeServerFactory;
import com.jd.blockchain.consensus.client.ClientFactory;
import com.jd.blockchain.consensus.manage.ManageClientFactory;
import com.jd.blockchain.consensus.service.NodeServerFactory;

public class BftsmartConsensusProvider implements ConsensusProvider {

	public static final String NAME = BftsmartConsensusProvider.class.getName();

	private static BftsmartSettingsFactory settingsFactory = new BftsmartSettingsFactory();

	private static BftsmartConsensusClientFactory clientFactory = new BftsmartConsensusClientFactory();

	private static BftsmartNodeServerFactory nodeServerFactory = new BftsmartNodeServerFactory();
	
	private static BftsmartConsensusManageClientFactory manangeClientFactory = new BftsmartConsensusManageClientFactory();

	public static final BftsmartConsensusProvider INSTANCE = new BftsmartConsensusProvider();

	private BftsmartConsensusProvider() {
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public SettingsFactory getSettingsFactory() {
		return settingsFactory;
	}

	@Override
	public ConsensusTypeEnum getConsensusType() {
		return ConsensusTypeEnum.BFTSMART;
	}

	@Override
	public ClientFactory getClientFactory() {
		return clientFactory;
	}

	@Override
	public NodeServerFactory getServerFactory() {
		return nodeServerFactory;
	}

	@Override
	public ManageClientFactory getManagerClientFactory() {
		return manangeClientFactory;
	}

}
