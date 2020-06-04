package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.Event;

import java.util.Iterator;

/**
 * 事件组；
 * 
 * @author huanghaiquan
 *
 */
public interface EventGroup {
	
	/**
	 * 获取事件序列；
	 * 
	 * @param eventName    事件名；
	 * @param fromSequence 开始序号；
	 * @param maxCount     最大数量；
	 * @return
	 */
	Iterator<Event> getEvents(String eventName, long fromSequence, int maxCount);
}