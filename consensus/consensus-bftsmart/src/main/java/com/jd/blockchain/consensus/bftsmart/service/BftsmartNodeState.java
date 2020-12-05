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

		/**
		 * 视图静态配置中指定的当前节点的 ID ；
		 * 
		 * @return
		 */
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

		/**
		 * 正在执行的共识ID；
		 * <p>
		 * 
		 * 如果没有共识在执行，则返回 -1 ；
		 * 
		 * @return
		 */
		int getConensusID();
		
		/**
		 * 上次执行过的共识ID；
		 * <p>
		 * 
		 * 如果从来没有共识执行过的话，则返回 -1;
		 * 
		 * @return
		 */
		int getLastConensusID();

		/**
		 * 共识领导者 ID ;
		 * 
		 * @return
		 */
		int getLeaderID();

	}

	public static interface CommunicationState {

		boolean isTomLayerThreadAlived();

		boolean isTomLayerRunning();

	}

}
