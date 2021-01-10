package com.jd.blockchain.ledger.core;

import utils.DataEntry;
import utils.Dataset;
import utils.SkippingIterator;
import utils.Transactional;

public interface MerkleDataset<K, V> extends Transactional, MerkleProvable<K>, Dataset<K, V> {

	boolean isReadonly();

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