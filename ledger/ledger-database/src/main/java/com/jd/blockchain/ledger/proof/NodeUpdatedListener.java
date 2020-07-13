package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;

public interface NodeUpdatedListener {
	
	void onUpdated(HashDigest nodeHash, MerkleTrieElement nodeEntry, byte[] nodeBytes);
	
}
