package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.proof.MerkleSequenceTree.DataNode;

import utils.Bytes;

public interface MerkleDataNodeEncoder {

	byte getFormatVersion();

	DataNode create(short hashAlgorithm, long sn, Bytes key, long version, byte[] hashedData);

	DataNode resolve(byte[] bytes);

}
