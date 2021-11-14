package com.jd.blockchain.consensus.bftsmart.manage;

import com.jd.blockchain.consensus.manage.ConsensusView;

import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.reconfiguration.views.View;
import utils.net.NetworkAddress;

public class BftsmartView implements ConsensusView {

	private View view;

	private Node[] nodes;

	public BftsmartView(View view) {
		this.view = view;
		int[] processes = view.getProcesses();
		this.nodes = new Node[processes.length];
		for (int i = 0; i < nodes.length; i++) {
			NodeNetwork network = view.getAddress(processes[i]);
			nodes[i] = new NodeInfo(processes[i], new NetworkAddress(network.getHost(), network.getConsensusPort(), network.isConsensusSecure()));
		}
	}

	@Override
	public int getViewID() {
		return view.getId();
	}

	@Override
	public Node[] getNodes() {
		return nodes.clone();
	}

	private static class NodeInfo implements Node {

		private int id;

		private NetworkAddress networkAddress;

		public NodeInfo(int id, NetworkAddress networkAddress) {
			this.id = id;
			this.networkAddress = networkAddress;
		}

		@Override
		public int getReplicaId() {
			return id;
		}

		@Override
		public NetworkAddress getNetworkAddress() {
			return networkAddress;
		}

	}

}
