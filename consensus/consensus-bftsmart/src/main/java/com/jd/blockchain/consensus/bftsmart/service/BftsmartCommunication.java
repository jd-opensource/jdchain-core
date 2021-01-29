package com.jd.blockchain.consensus.bftsmart.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.service.Communication;
import com.jd.blockchain.consensus.service.NodeSession;

import bftsmart.tom.core.TOMLayer;

public class BftsmartCommunication implements Communication {

	private TOMLayer tomLayer;

	private BftsmartServerSettings serverSettings;

	private Map<String, BftsmartNodeSession> targetSessions = new ConcurrentHashMap<>();

	public BftsmartCommunication(TOMLayer tomLayer, BftsmartNodeServer nodeServer) {
		this.tomLayer = tomLayer;
		this.serverSettings = (BftsmartServerSettings) nodeServer.getServerSettings();
	}

	@Override
	public NodeSession getSession(String target) {
		BftsmartNodeSession session = targetSessions.get(target);
		if (session != null) {
			return session;
		}
		return getSession(target, true);
	}

	private synchronized NodeSession getSession(String target, boolean created) {
		BftsmartNodeSession session = targetSessions.get(target);
		if (session != null) {
			return session;
		}
		if (created) {
			BftsmartNodeSettings targetNodeSetting = null;
			BftsmartConsensusViewSettings viewSettings = serverSettings.getConsensusSettings();
			NodeSettings[] nodeSettings = viewSettings.getNodes();
			for (NodeSettings ns : nodeSettings) {
				if (ns.getAddress().equals(target)) {
					targetNodeSetting = (BftsmartNodeSettings) ns;
				}
			}
			if (targetNodeSetting == null) {
				return null;
			}
			session = new BftsmartNodeSession(serverSettings.getReplicaSettings().getAddress(),
					targetNodeSetting.getId(), target);
			targetSessions.put(target, session);
			return session;
		}
		return null;
	}

	/**
	 * 节点服务间的会话；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class BftsmartNodeSession implements NodeSession {

		private int targetId;

		private String source;

		private String target;

		public BftsmartNodeSession(String source, int targetId, String target) {
			this.source = source;
			this.targetId = targetId;
			this.target = target;
		}

		@Override
		public String getSource() {
			return source;
		}

		@Override
		public String getTarget() {
			return target;
		}

		@Override
		public void reset() {
			//TODO:
//			tomLayer.getCommunication().getServersCommunication().resetConnection(targetId);
		}

	}

}
