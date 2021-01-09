package com.jd.blockchain.gateway.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.gateway.GatewayServerBooter;
import com.jd.blockchain.sdk.service.SessionCredentialProvider;

import utils.io.Storage;

@Component
public class GatewayConsensusCredentialProvider implements SessionCredentialProvider {

	@Autowired
	private Storage runtimeStorage;

	private Storage credentialStorage;

	@PostConstruct
	private void init() {
		this.credentialStorage = runtimeStorage.getStorage("consensus").getStorage("credentials");
	}

	@Override
	public SessionCredential getCredential(String key) {
		byte[] bytes = credentialStorage.readBytes(key);
		if (bytes == null) {
			return null;
		}
		return BinaryProtocol.decode(bytes);
	}

	@Override
	public void setCredential(String key, SessionCredential credential) {
		byte[] bytes = BinaryProtocol.encode(credential);
		credentialStorage.writeBytes(key, bytes);
	}

}
