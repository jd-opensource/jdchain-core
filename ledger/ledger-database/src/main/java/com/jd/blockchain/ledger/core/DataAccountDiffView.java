package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.utils.DataEntry;
import com.jd.blockchain.utils.SkippingIterator;

public interface DataAccountDiffView extends AccountDiffView {
	
	SkippingIterator<DataEntry<String, BytesValue>> getHeaderDiff();
	
	SkippingIterator<DataEntry<String, BytesValue>> getDataDiff();
	
}
