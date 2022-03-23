package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.MerkleProof;

import utils.Bytes;
import utils.EmptySkippingIterator;
import utils.SkippingIterator;

public class EmptyAccountSet<T> implements BaseAccountSet<T> {
	
	@Override
	public HashDigest getRootHash() {
		return null;
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return null;
	}
	
	@Override
	public SkippingIterator<BlockchainIdentity> identityIterator() {
		return EmptySkippingIterator.instance();
	}
	
	@Override
	public long getTotal() {
		return 0;
	}

	@Override
	public boolean contains(Bytes address) {
		return false;
	}

	@Override
	public T getAccount(String address) {
		return null;
	}

	@Override
	public T getAccount(Bytes address) {
		return null;
	}

	@Override
	public T getAccount(Bytes address, long version) {
		return null;
	}

}
