package com.jd.blockchain.ledger.core;

import com.jd.blockchain.utils.Bytes;

public class EventGroup {
	
	
	private MerkleDataSet events;
	
	
	
	/**
	 * 发布事件；<br>
	 * 
	 * @param eventName 事件名；
	 * @param message 消息内容；
	 * @param sequence 事件序号；
	 * @return 
	 */
	private long publish(String eventName, byte[] message, long sequence) {
		Bytes key = encodeKey(eventName);
		return events.setValue(key, message, sequence);
	}



	private Bytes encodeKey(String eventName) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
