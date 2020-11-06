package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.EmptySkippingIterator;
import com.jd.blockchain.utils.SkippingIterator;

public class EmptyAccountSet<T> implements MerkleAccountCollection<T> {
	
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
