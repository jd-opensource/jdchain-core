package com.jd.blockchain.tools.initializer.web;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.ParticipantNode;

import com.jd.blockchain.ledger.ParticipantNodeState;
import utils.Bytes;

import java.util.ArrayList;
import java.util.List;

public class ParticipantReplica implements Replica {
	
	private ParticipantNode parti;
	
	public ParticipantReplica(ParticipantNode parti) {
		this.parti = parti;
	}

	@Override
	public int getId() {
		return parti.getId();
	}

	@Override
	public Bytes getAddress() {
		return parti.getAddress();
	}

	@Override
	public String getName() {
		return parti.getName();
	}

	@Override
	public PubKey getPubKey() {
		return parti.getPubKey();
	}
	
	public static Replica[] wrap(ParticipantNode... participants) {
		List<ParticipantReplica> partis = new ArrayList<>();
		for (int i = 0; i < participants.length; i++) {
			if(participants[i].getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {
				partis.add(new ParticipantReplica(participants[i]));
			}
		}
		return partis.toArray(new ParticipantReplica[0]);
	}
	
	
}
