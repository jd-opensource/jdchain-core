package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.crypto.HashDigest;

public interface MerkleEntrySelector {

	void accept(HashDigest nodeHash, MerkleIndex nodePath);

	void accept(HashDigest nodeHash, long id, byte[] bytesValue);

}