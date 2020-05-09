package com.jd.blockchain.peer.decorator;

import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.ParticipantNodeState;
import com.jd.blockchain.utils.Bytes;

public class ParticipantNodeDecorator implements ParticipantNode {

    private int id;

    private Bytes address;

    private String name;

    private PubKey pubKey;

    private ParticipantNodeState participantNodeState;

    public ParticipantNodeDecorator(ParticipantNode participantNode) {
        this.id = participantNode.getId();
        this.address = participantNode.getAddress();
        this.name = participantNode.getName();
        this.pubKey = participantNode.getPubKey();
        this.participantNodeState = participantNode.getParticipantNodeState();
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public Bytes getAddress() {
        return this.address;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public PubKey getPubKey() {
        return this.pubKey;
    }

    @Override
    public ParticipantNodeState getParticipantNodeState() {
        return this.participantNodeState;
    }
}
