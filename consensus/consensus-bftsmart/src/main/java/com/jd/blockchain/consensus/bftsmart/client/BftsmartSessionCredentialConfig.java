package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.ConsensusTypeEnum;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;

public class BftsmartSessionCredentialConfig implements BftsmartSessionCredential {

	private int id;

	private int range;

	private long createdTime;
	
	public BftsmartSessionCredentialConfig(int id, int range, long createdTime) {
		this.id = id;
		this.range = range;
		this.createdTime = createdTime;
	}

	@Override
	public int getClientId() {
		return id;
	}

	@Override
	public int getClientIdRange() {
		return range;
	}

	@Override
	public long getCreatedTime() {
		return createdTime;
	}

	public static BftsmartSessionCredential createEmptyCredential() {
		return new BftsmartSessionCredentialConfig(-1, 0, System.currentTimeMillis());
	}

	@Override
	public int consensusProviderType() {
		return ConsensusTypeEnum.BFT.getCode();
	}
}
