package com.jd.blockchain.peer.ledger.service.utils;

import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerMetadata_V2;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.ParticipantNode;

public class LedgerAdminInfoDecorator implements LedgerAdminInfo {

    private LedgerMetadata_V2 metadata;

    private LedgerSettings settings;

    private ParticipantNode[] participants;

    private long participantCount;


    public LedgerAdminInfoDecorator(LedgerAdminInfo ledgerAdminInfo) {
        metadata = ledgerAdminInfo.getMetadata();
        participantCount = ledgerAdminInfo.getParticipantCount();

        initSettings(ledgerAdminInfo.getSettings());
        initParticipants(ledgerAdminInfo.getParticipants());
    }

    private void initParticipants(ParticipantNode[] participantNodes) {
        if (participantNodes != null && participantNodes.length > 0) {
            participants = new ParticipantNode[participantNodes.length];
            for (int i = 0; i < participantNodes.length; i++) {
                ParticipantNode node = participantNodes[i];
                participants[i] = new ParticipantNodeDecorator(node);
            }
        }
    }

    private void initSettings(LedgerSettings ledgerSettings) {
        if (ledgerSettings != null) {
            settings = new LedgerSettingsDecorator(ledgerSettings);
        }
    }

    @Override
    public LedgerMetadata_V2 getMetadata() {
        return metadata;
    }

    @Override
    public LedgerSettings getSettings() {
        return settings;
    }

    @Override
    public ParticipantNode[] getParticipants() {
        return participants;
    }

    @Override
    public long getParticipantCount() {
        return participantCount;
    }
}
