package com.jd.blockchain.ledger.core;

import utils.DataEntry;
import utils.Dataset;
import utils.SkippingIterator;
import utils.Transactional;

public interface IteratorDataset<K, V> extends Transactional, MerkleProvable<K>, Dataset<K, V> {

	/**
	 * Ascending iterator；
	 * 
	 * @return
	 */
	SkippingIterator<DataEntry<K, V>> iterator();

	/**
	 * Descending iterator；
	 * 
	 * @return
	 */
	SkippingIterator<DataEntry<K, V>> iteratorDesc();

}