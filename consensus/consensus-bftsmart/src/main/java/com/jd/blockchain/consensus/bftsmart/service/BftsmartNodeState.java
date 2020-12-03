package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.service.NodeState;

public interface BftsmartNodeState extends NodeState {

	/**
	 * 当前节点 ID；
	 * 
	 * @return
	 */
	int getNodeID();

	/**
	 * 是否是领导者；
	 * 
	 * @return
	 */
	default boolean isLeader() {
		return getNodeID() == getLeaderState().getLeaderID();
	}

	ViewState getViewState();

	LeaderState getLeaderState();

	ConsensusState getConsensusState();
	
	CommunicationState getCommunicationState();

	// ------------------------------------------------

	public static interface ViewState {

		int getViewID();
		
		int getStaticConfProccessID();
		
		int getViewF();
		
		int getViewN();
		
		int getQuorum();

		int[] getProcessIDs();

		NodeNetworkAddress[] getProcessNetAddresses();

	}

	public static interface LeaderState {

		int getLeaderID();

		int getLastRegency();

		int getNextRegency();

	}

	public static interface ConsensusState {

		int getConensusID();

		int getLeaderID();

		int getLastConensusID();

	}
	
	public static interface CommunicationState {
		
		boolean isTomLayerThreadAlived();
		
		boolean isTomLayerRunning();
		
	}

}
