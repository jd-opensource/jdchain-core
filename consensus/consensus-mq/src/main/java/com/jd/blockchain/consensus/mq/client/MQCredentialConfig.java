package com.jd.blockchain.consensus.mq.client;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ConsensusTypeEnum;

public class MQCredentialConfig implements MQCredentialInfo{

	static {
		DataContractRegistry.register(MQCredentialInfo.class);
	}

	private byte[] info;
	
	public MQCredentialConfig(byte[] info) {
		this.info = info;
	}

	@Override
	public byte[] getInfo() {
		return info;
	}

	public void setInfo(byte[] info) {
		this.info = info;
	}

	public static MQCredentialConfig createEmptyCredential() {
		return new MQCredentialConfig(new byte[]{});
	}

	@Override
	public int consensusProviderType() {
		return ConsensusTypeEnum.MQ.getCode();
	}
}
