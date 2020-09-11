package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.ledger.merkletree.MerkleSortTree.ValueEntry;

class IDValue<T> implements ValueEntry<T> {

	private long id;

	private T value;

	IDValue(long id, T value) {
		this.id = id;
		this.value = value;
	}

	@Override
	public long getId() {
		return id;
	}

	/**
	 * 数据字节；
	 * 
	 * @return
	 */
	@Override
	public T getValue() {
		return value;
	}

}
