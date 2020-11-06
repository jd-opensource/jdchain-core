package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.LedgerAdminSettings;

public interface LedgerAdminDataQuery {
	
	LedgerAdminSettings getAdminInfo();

	ParticipantCollection getParticipantDataset();

}