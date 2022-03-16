package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.service.*;
import utils.GmSSLProvider;
import utils.io.Storage;
import utils.net.NetworkAddress;
import utils.net.SSLSecurity;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class BftsmartNodeServerFactory implements NodeServerFactory {

	private static Map<String, NodeSettings[]> nodeServerMap = new ConcurrentHashMap<>();

	@Override
	public ServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings,
			String nodeAddress) {
		return buildServerSettings(realmName, viewSettings, nodeAddress, new SSLSecurity(), null);
	}

	@Override
	public ServerSettings buildServerSettings(String realmName, ConsensusViewSettings viewSettings, String nodeAddress, SSLSecurity sslSecurity, Properties properties) {
		NodeSettings currentNodeSetting = null;

		BftsmartServerSettingConfig serverSettings = new BftsmartServerSettingConfig();

		// find current node according to current address
		for (NodeSettings nodeSettings : viewSettings.getNodes()) {
			if (nodeSettings.getAddress().equals(nodeAddress)) {
				currentNodeSetting = nodeSettings;
				break;
			}
		}

		if (currentNodeSetting == null) {
			throw new IllegalArgumentException("Node address does not exist in view settings!");
		}

		// set server settings
		serverSettings.setRealmName(realmName);
		serverSettings.setReplicaSettings(currentNodeSetting);
		serverSettings.setConsensusSettings((BftsmartConsensusViewSettings) viewSettings);
		serverSettings.setSslSecurity(sslSecurity);

		GmSSLProvider.enableGMSupport(sslSecurity.getProtocol());

		return serverSettings;
	}

	@Override
	public NodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler,
			StateMachineReplicate stateMachineReplicator, Storage runtimeStorage) {

		BftsmartServerSettings consensusSettings = (BftsmartServerSettings) serverSettings;

		NodeSettings[] currNodeSettings = consensusSettings.getConsensusSettings().getNodes();

		String currRealName = serverSettings.getRealmName();
		

		// check conflict realm
		if (!hasIntersection(currRealName, currNodeSettings)) {
			Storage nodeRuntimeStorage = runtimeStorage.getStorage("bftsmart");
			BftsmartNodeServer nodeServer = new BftsmartNodeServer(serverSettings, messageHandler,
					stateMachineReplicator, nodeRuntimeStorage, consensusSettings.getSslSecurity());
			nodeServerMap.put(serverSettings.getRealmName(), currNodeSettings);
			return nodeServer;
		} else {
			throw new IllegalArgumentException("setupServer serverSettings parameters error!");

		}
	}

	// check if consensus realm conflict, by this support multi ledgers
	private boolean hasIntersection(String currRealName, NodeSettings[] currNodeSettings) {

		// first check if is same consensus realm
		for (String existRealmName : nodeServerMap.keySet()) {
			if (currRealName.equals(existRealmName)) {
				return false;
			}
		}
		// check conflict
		for (NodeSettings[] existNodeSettings : nodeServerMap.values()) {
			for (NodeSettings curr : currNodeSettings) {
				for (NodeSettings exist : existNodeSettings) {
					if (((BftsmartNodeSettings) curr).getNetworkAddress()
							.equals(((BftsmartNodeSettings) exist).getNetworkAddress())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	// compute hashcode for consensus nodes
	private int getHashcode(NodeSettings[] nodeSettings) {

		int i = 0;
		NetworkAddress[] nodeAddrs = new NetworkAddress[nodeSettings.length];
		for (NodeSettings setting : nodeSettings) {

			nodeAddrs[i++] = ((BftsmartNodeSettings) setting).getNetworkAddress();
		}
		int hashCode = Arrays.hashCode(nodeAddrs);
		return hashCode;

	}

}
