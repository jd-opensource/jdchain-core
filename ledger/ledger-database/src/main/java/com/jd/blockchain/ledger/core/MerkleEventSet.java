package com.jd.blockchain.ledger.core;

import java.util.Iterator;

import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.utils.Bytes;

public class MerkleEventSet implements EventGroup, EventPublisher {
	
	private MerkleDataSet events;
	
	
	
	/**
	 * 发布事件；<br>
	 * 
	 * @param eventName 事件名；
	 * @param message 消息内容；
	 * @param sequence 事件序号；
	 * @return 
	 */
	@Override
	public long publish(String eventName, BytesValue message, long sequence) {
		Bytes key = encodeKey(eventName);
		return events.setValue(key, message.getBytes().toBytes(), sequence);
	}

	@Override
	public Iterator<Event> getEvents(String eventName, String fromSequence, int maxCount) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Bytes encodeKey(String eventName) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
