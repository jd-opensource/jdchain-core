package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.utils.SkippingIterator;

public interface EventAccountDiffView extends AccountDiffView {

	SkippingIterator<Event> getEventDiff();

}
