package com.jd.blockchain.gateway.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.gateway.GatewayServerBooter;
import com.jd.blockchain.sdk.service.SessionCredentialProvider;
import com.jd.blockchain.utils.io.Storage;

@Component
public class GatewayConsensusCredentialProvider implements SessionCredentialProvider {

	@Autowired
	private Storage homeStorage;

	private Storage credentialStorage;

	@PostConstruct
	private void init() {
		this.credentialStorage = homeStorage.getStorage(GatewayServerBooter.RUNTIME_FOLDER_NAME)
				.getStorage("consensus").getStorage("credentials");
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
