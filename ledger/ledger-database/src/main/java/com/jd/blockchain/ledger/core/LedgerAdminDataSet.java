package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.LedgerAdminSettings;

public interface LedgerAdminDataSet {
	
	LedgerAdminSettings getAdminSettings();

	ParticipantCollection getParticipantDataset();

}