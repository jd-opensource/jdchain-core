package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.crypto.PubKey;

import bftsmart.demo.bftmap.BFTMapRequestType;

public class BftsmartClientConfig implements BftsmartClientSettings {

	private int clientId;
	private PubKey clientPubkey;
	private ConsensusViewSettings viewSettings;
	private byte[] topology;
	private byte[] tomConfig;
	BftsmartClientIncomingSettings clientIncomingSettings;
	private BftsmartSessionCredential sessionCredential;

	/**
	 * 创建客户端配置；
	 * 
	 * @param clientPubkey   客户端的公钥；
	 * @param sessionCredential 回复的客户端信息；
	 * @param viewSettings
	 * @param topology
	 * @param tomConfig
	 */
	public BftsmartClientConfig(PubKey clientPubkey, BftsmartSessionCredential sessionCredential, ConsensusViewSettings viewSettings,
			byte[] topology, byte[] tomConfig) {
		this.clientId = sessionCredential.getClientId();
		this.clientPubkey = clientPubkey;
		this.viewSettings = viewSettings;
		this.topology = topology;
		this.tomConfig = tomConfig;
		this.sessionCredential = sessionCredential;
	}

	public BftsmartClientConfig(BftsmartClientIncomingSettings clientIncomingSettings) {
		this.clientIncomingSettings = clientIncomingSettings;
		this.clientId = clientIncomingSettings.getClientId();
		this.clientPubkey = clientIncomingSettings.getPubKey();
		this.viewSettings = clientIncomingSettings.getViewSettings();
		this.topology = clientIncomingSettings.getTopology();
		this.tomConfig = clientIncomingSettings.getTomConfig();
		this.sessionCredential = (BftsmartSessionCredential) clientIncomingSettings.getCredential();
	}

	@Override
	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	@Override
	public PubKey getClientPubKey() {
		return clientPubkey;
	}

	public void setClientPubkey(PubKey clientPubkey) {
		this.clientPubkey = clientPubkey;
	}

	@Override
	public ConsensusViewSettings getViewSettings() {
		return viewSettings;
	}

	public void setConsensusSettings(ConsensusViewSettings consensusSettings) {
		this.viewSettings = consensusSettings;
	}

	public byte[] getTopology() {
		return topology;
	}

	public void setTopology(byte[] topology) {
		this.topology = topology;
	}

	@Override
	public byte[] getTomConfig() {
		return tomConfig;
	}

	public void setTomConfig(byte[] tomConfig) {
		this.tomConfig = tomConfig;
	}

	@Override
	public BftsmartSessionCredential getSessionCredential() {
		return sessionCredential;
	}
}
