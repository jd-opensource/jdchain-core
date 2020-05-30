package com.jd.blockchain.peer.consensus;

import java.io.InputStream;
import java.util.Iterator;

import bftsmart.reconfiguration.views.View;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.consensus.service.StateSnapshot;
import org.springframework.stereotype.Component;

@Component
public class LedgerStateManager implements StateMachineReplicate{

	private long latestStateId;
	private int latestViewId;

	@Override
	public long getLatestStateID(String realmName) {
		return latestStateId;
	}

	@Override
	public int getLatestViewID(String realmName) {
		return latestViewId;
	}


	public void setLatestStateId(long latestStateId) {
		this.latestStateId = latestStateId;
	}

	public void setLatestViewId(int latestViewId) {
		this.latestViewId = latestViewId;
	}

	@Override
	public StateSnapshot getSnapshot(String realmName, long stateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<StateSnapshot> getSnapshots(String realmName, long fromStateId, long toStateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readState(String realmName, long stateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setupState(String realmName, StateSnapshot snapshot, InputStream state) {
		// TODO Auto-generated method stub
		
	}

}
