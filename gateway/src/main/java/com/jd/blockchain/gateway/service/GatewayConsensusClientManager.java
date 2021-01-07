package com.jd.blockchain.gateway.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.bftsmart.client.BftsmartConsensusClient;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.sdk.service.ConsensusClientManager;

@Component
public class GatewayConsensusClientManager implements ConsensusClientManager {

	private Map<HashDigest, ConsensusClient> ledgerConsensusClients = new ConcurrentHashMap<HashDigest, ConsensusClient>();

	@Override
	public synchronized ConsensusClient getConsensusClient(HashDigest ledgerHash, SessionCredential sessionCredential,
			ConsensusClientFactory factory) {
		ConsensusClient client = ledgerConsensusClients.get(ledgerHash);
		if (client == null) {
			client = factory.create();
			client.connect();
			ledgerConsensusClients.put(ledgerHash, client);
		} else {
			if (isCredentialUpated(client, sessionCredential)) {
				client.close();
				ledgerConsensusClients.remove(ledgerHash);

				client = factory.create();
				ledgerConsensusClients.put(ledgerHash, client);
			}
		}
		return client;
	}
	
	@Override
	public void reset() {
		ConsensusClient[] pooledClients = ledgerConsensusClients.values().toArray(new ConsensusClient[ledgerConsensusClients.size()]);
		ledgerConsensusClients.clear();
		for (ConsensusClient client : pooledClients) {
			client.close();
		}
	}

	private boolean isCredentialUpated(ConsensusClient client, SessionCredential sessionCredential) {
		if (client instanceof BftsmartConsensusClient && sessionCredential instanceof BftsmartSessionCredential) {
			BftsmartConsensusClient bftsmartClient = (BftsmartConsensusClient) client;
			BftsmartSessionCredential newCredential = (BftsmartSessionCredential) sessionCredential;

			BftsmartSessionCredential oldCredential = bftsmartClient.getSettings().getSessionCredential();

			// clientId 和 clientIdRange 任何一个有差异，都表示凭证已更新；
			return oldCredential.getClientId() != newCredential.getClientId()
					|| oldCredential.getClientIdRange() != newCredential.getClientIdRange();
		}
		return true;
	}

}
