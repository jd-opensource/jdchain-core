package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.NodeServerFactory;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.utils.net.NetworkAddress;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BftsmartNodeServerFactory implements NodeServerFactory {

	private static Map<String, NodeSettings[]> nodeServerMap = new ConcurrentHashMap<>();

	@Override
	public ServerSettings buildServerSettings(String realmName, ConsensusSettings consensusSetting, Replica replica) {

		NodeSettings serverNode = null;

		BftsmartServerSettingConfig serverSettings = new BftsmartServerSettingConfig();

		// find current node according to current address
		for (NodeSettings nodeSettings : consensusSetting.getNodes()) {
			if (nodeSettings.getAddress().equals(replica.getAddress().toBase58())) {
				serverNode = nodeSettings;
				break;
			}
		}

		if (serverNode == null) {
			throw new IllegalArgumentException();
		}

		// set server settings
		serverSettings.setRealmName(realmName);

		serverSettings.setReplicaSettings(serverNode);

		serverSettings.setConsensusSettings((BftsmartConsensusSettings) consensusSetting);

		return serverSettings;

	}

	@Override
	public NodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler,
			StateMachineReplicate stateMachineReplicator) {

		NodeSettings[] currNodeSettings = (((BftsmartServerSettings) serverSettings).getConsensusSettings()).getNodes();

		String currRealName = serverSettings.getRealmName();

		// check conflict realm
		if (!hasIntersection(currRealName, currNodeSettings)) {
			BftsmartNodeServer nodeServer = new BftsmartNodeServer(serverSettings, messageHandler,
					stateMachineReplicator);
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
