package com.jd.blockchain.consensus.raft.client;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ConsensusTypeEnum;

public class RaftSessionCredentialConfig implements RaftSessionCredential {

	static {
		DataContractRegistry.register(RaftSessionCredential.class);
	}

	private byte[] info;
	
	public RaftSessionCredentialConfig(byte[] info) {
		this.info = info;
	}

	@Override
	public byte[] getInfo() {
		return info;
	}

	public void setInfo(byte[] info) {
		this.info = info;
	}

	public static RaftSessionCredentialConfig createEmptyCredential() {
		return new RaftSessionCredentialConfig(new byte[]{});
	}

	@Override
	public int consensusProviderType() {
		return ConsensusTypeEnum.RAFT.getCode();
	}
}
