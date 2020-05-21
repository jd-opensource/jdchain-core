package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BytesValue;

public interface EventPublisher extends EventGroup {

	/**
	 * 发布事件；<br>
	 * 
	 * @param eventName      事件名；
	 * @param content        消息内容；
	 * @param latestSequence 该事件序列的最新序号；
	 * @return
	 */
	long publish(String eventName, BytesValue content, long latestSequence);

}