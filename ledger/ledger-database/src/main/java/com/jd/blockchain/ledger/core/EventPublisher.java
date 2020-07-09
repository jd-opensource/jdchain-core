package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.Event;

public interface EventPublisher {

	/**
	 * 发布事件
	 * @param event
	 * @return
	 */
	long publish(Event event);

}