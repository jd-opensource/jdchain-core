package com.jd.blockchain.consensus.mq.client;

public class MQCredentialConfig implements MQCredentialInfo{
	
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

}
