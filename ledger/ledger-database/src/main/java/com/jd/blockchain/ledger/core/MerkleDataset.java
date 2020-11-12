package com.jd.blockchain.ledger.core;

import com.jd.blockchain.utils.DataEntry;
import com.jd.blockchain.utils.Dataset;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.Transactional;

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