package com.jd.blockchain.ledger.core;

import utils.DataEntry;
import utils.Dataset;
import utils.SkippingIterator;
import utils.Transactional;

public interface IteratorDataset<K, V> extends Transactional, MerkleProvable<K>, Dataset<K, V> {

	/**
	 * Ascending identity iterator；
	 * 
	 * @return
	 */
	SkippingIterator<DataEntry<K, V>> idIterator();

	/**
	 * Ascending kv iterator；
	 *
	 * @return
	 */
	SkippingIterator<DataEntry<K, V>> kvIterator();

	/**
	 * Descending identity iterator；
	 * 
	 * @return
	 */
	SkippingIterator<DataEntry<K, V>> idIteratorDesc();

	/**
	 * Descending kv iterator；
	 *
	 * @return
	 */
	SkippingIterator<DataEntry<K, V>> kvIteratorDesc();

}