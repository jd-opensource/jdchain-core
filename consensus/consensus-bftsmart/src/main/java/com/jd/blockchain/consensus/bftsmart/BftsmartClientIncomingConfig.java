package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.crypto.PubKey;

public class BftsmartClientIncomingConfig implements BftsmartClientIncomingSettings {

    private BftsmartConsensusViewSettings consensusSettings;

    private byte[] topology;

    private byte[] tomConfig;

    private int clientId;

    private PubKey pubKey;

	private BftsmartSessionCredential sessionCredential;

    
    @Override
    public ConsensusViewSettings getViewSettings() {
    	return consensusSettings;
    }

    public void setViewSettings(BftsmartConsensusViewSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
    }

    @Override
    public byte[] getTopology() {
        return topology;
    }

    public void setTopology(byte[] topology) {
        this.topology = topology;
    }

    @Override
    public int getClientId() {
        return clientId;
    }

    @Override
    public String getProviderName() {
        return BftsmartConsensusProvider.NAME;
    }

    @Override
    public byte[] getTomConfig() {
        return tomConfig;
    }

    public void setTomConfig(byte[] tomConfig) {
        this.tomConfig = tomConfig;
    }

    @Override
    public PubKey getPubKey() {
        return pubKey;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }
    
    @Override
    public BftsmartSessionCredential getCredential() {
    	return sessionCredential;
    }
    
    public void setSessionCredential(BftsmartSessionCredential sessionCredential) {
    	this.clientId = sessionCredential.getClientId();
		this.sessionCredential = sessionCredential;
	}
    
}
