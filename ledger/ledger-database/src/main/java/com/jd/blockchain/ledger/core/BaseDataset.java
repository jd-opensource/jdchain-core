package com.jd.blockchain.ledger.core;


public interface BaseDataset<K, V> extends IteratorDataset<K, V> {

	boolean isReadonly();

}