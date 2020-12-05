package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.service.NodeServer;

import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.tom.ServiceReplica;

class BftsmartNodeStateExporter implements BftsmartNodeState {

	private ServiceReplica replica;

	private NodeServer nodeServer;

	private ViewStateImpl viewState;

	private LeaderStateImpl leaderState;

	private ConsensusStateImpl consensusState;

	private CommunicationStateImpl communicationState;

	public BftsmartNodeStateExporter(ServiceReplica replica, NodeServer nodeServer) {
		this.replica = replica;
		this.nodeServer = nodeServer;
		this.viewState = new ViewStateImpl();
		this.leaderState = new LeaderStateImpl();
		this.consensusState = new ConsensusStateImpl();
		this.communicationState = new CommunicationStateImpl();
	}

	@Override
	public boolean isRunning() {
		return nodeServer.isRunning();
	}

	@Override
	public int getNodeID() {
		return replica.getId();
	}

	@Override
	public ViewState getViewState() {
		return viewState;
	}

	@Override
	public LeaderState getLeaderState() {
		return leaderState;
	}

	@Override
	public ConsensusState getConsensusState() {
		return consensusState;
	}

	@Override
	public CommunicationState getCommunicationState() {
		return communicationState;
	}

	private class ViewStateImpl implements ViewState {

		@Override
		public int getViewID() {
			return replica.getViewController().getCurrentViewId();
		}

		@Override
		public int getStaticConfProccessID() {
			return replica.getViewController().getStaticConf().getProcessId();
		}

		@Override
		public int getQuorum() {
			return replica.getViewController().getQuorum();
		}

		@Override
		public int getViewN() {
			return replica.getViewController().getCurrentViewN();
		}

		@Override
		public int getViewF() {
			return replica.getViewController().getCurrentViewF();
		}

		@Override
		public int[] getProcessIDs() {
			int[] processIDs = replica.getViewController().getCurrentViewProcesses();
			return processIDs.clone();
		}

		@Override
		public NodeNetworkAddress[] getProcessNetAddresses() {
			int[] procIds = replica.getViewController().getCurrentViewProcesses();

			NodeNetworkAddress[] networkAddresses = new NodeNetworkAddress[procIds.length];
			for (int i = 0; i < procIds.length; i++) {
				NodeNetwork nodeNetwork = replica.getViewController().getRemoteAddress(procIds[i]);
				if (nodeNetwork == null) {
					continue;
				}
				networkAddresses[i] = new BftsmartNodeServer.PeerNodeNetwork(nodeNetwork.getHost(),
						nodeNetwork.getConsensusPort(), nodeNetwork.getMonitorPort());
			}

			return networkAddresses;
		}

	}

	private class LeaderStateImpl implements LeaderState {

		@Override
		public int getLeaderID() {
			return replica.getTomLayer().getSynchronizer().getLCManager().getCurrentLeader();
		}

		@Override
		public int getLastRegency() {
			return replica.getTomLayer().getSynchronizer().getLCManager().getLastReg();
		}

		@Override
		public int getNextRegency() {
			return replica.getTomLayer().getSynchronizer().getLCManager().getNextReg();
		}
		
	}

	private class ConsensusStateImpl implements ConsensusState {

		@Override
		public int getConensusID() {
			return replica.getTomLayer().getInExec();
		}
		
		@Override
		public int getLastConensusID() {
			return replica.getTomLayer().getLastExec();
		}
		
		@Override
		public int getLeaderID() {
			return replica.getTomLayer().getExecManager().getCurrentLeader();
		}

	}
	
	
	private class CommunicationStateImpl implements CommunicationState{

		@Override
		public boolean isTomLayerThreadAlived() {
			return replica.getTomLayer().isAlive();
		}

		@Override
		public boolean isTomLayerRunning() {
			return replica.getTomLayer().isRunning();
		}
		
	}

}
