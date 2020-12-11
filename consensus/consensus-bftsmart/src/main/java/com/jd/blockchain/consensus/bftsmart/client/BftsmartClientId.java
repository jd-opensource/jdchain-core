package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.bftsmart.BftsmartCredentialInfo;

public class BftsmartClientId implements BftsmartCredentialInfo {
	
	private int id;
	
	private int range;
	
	public BftsmartClientId(int id, int range) {
		this.id = id;
		this.range = range;
	}

	@Override
	public int getClientId() {
		return id;
	}

	@Override
	public int getClientIdRange() {
		return range;
	}

}
