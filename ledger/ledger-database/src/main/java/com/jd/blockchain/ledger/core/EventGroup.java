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
	Event[] getEvents(String eventName, long fromSequence, int maxCount);

	/**
	 * 查询所有事件名称，返回最新事件
	 *
	 * @param fromIndex
	 * @param count
	 * @return
	 */
	Event[] getEventNames(long fromIndex, int count);

	/**
	 * 事件名称总数
	 *
	 * @return
	 */
	long totalEventNames();

	/**
	 * 事件总数
	 *
	 * @param eventName
	 * @return
	 */
	long totalEvents(String eventName);
}