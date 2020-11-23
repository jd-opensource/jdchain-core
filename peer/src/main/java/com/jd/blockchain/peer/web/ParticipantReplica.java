package com.jd.blockchain.peer.web;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.utils.Bytes;

public class ParticipantReplica implements Replica {

	private ParticipantNode participant;

	public ParticipantReplica(ParticipantNode participant) {
		this.participant = participant;
	}

	@Override
	public int getId() {
		return participant.getId();
	}

	@Override
	public Bytes getAddress() {
		return participant.getAddress();
	}

	@Override
	public String getName() {
		return participant.getName();
	}

	@Override
	public PubKey getPubKey() {
		return participant.getPubKey();
	}

}
