package com.jd.blockchain.ledger.core;

import utils.DataEntry;
import utils.Dataset;
import utils.SkippingIterator;
import utils.Transactional;

public interface BaseDataset<K, V> extends IteratorDataset<K, V> {

	boolean isReadonly();

}