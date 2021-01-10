package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BytesValue;

import utils.DataEntry;
import utils.SkippingIterator;

public interface DataAccountDiffView extends AccountDiffView {
	
	SkippingIterator<DataEntry<String, BytesValue>> getHeaderDiff();
	
	SkippingIterator<DataEntry<String, BytesValue>> getDataDiff();
	
}
