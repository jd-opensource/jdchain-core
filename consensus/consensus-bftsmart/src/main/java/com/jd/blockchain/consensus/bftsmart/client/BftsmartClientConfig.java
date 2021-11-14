package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.crypto.PubKey;

import bftsmart.demo.bftmap.BFTMapRequestType;
import utils.net.SSLSecurity;

public class BftsmartClientConfig implements BftsmartClientSettings {

	private int clientId;
	private PubKey clientPubkey;
	private ConsensusViewSettings viewSettings;
	private byte[] topology;
	private byte[] tomConfig;
	BftsmartClientIncomingSettings clientIncomingSettings;
	private BftsmartSessionCredential sessionCredential;
	private SSLSecurity sslSecurity;

	public BftsmartClientConfig(BftsmartClientIncomingSettings clientIncomingSettings) {
		this(clientIncomingSettings, new SSLSecurity());
	}

	public BftsmartClientConfig(BftsmartClientIncomingSettings clientIncomingSettings, SSLSecurity sslSecurity) {
		this.clientIncomingSettings = clientIncomingSettings;
		this.clientId = clientIncomingSettings.getClientId();
		this.clientPubkey = clientIncomingSettings.getPubKey();
		this.viewSettings = clientIncomingSettings.getViewSettings();
		this.topology = clientIncomingSettings.getTopology();
		this.tomConfig = clientIncomingSettings.getTomConfig();
		this.sessionCredential = (BftsmartSessionCredential) clientIncomingSettings.getCredential();
		this.sslSecurity = sslSecurity;
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

	@Override
	public SSLSecurity getSSLSecurity() {
		return sslSecurity;
	}

	public SSLSecurity getSslSecurity() {
		return sslSecurity;
	}

	public void setSslSecurity(SSLSecurity sslSecurity) {
		this.sslSecurity = sslSecurity;
	}
}
