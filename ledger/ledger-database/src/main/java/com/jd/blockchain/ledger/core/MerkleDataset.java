package com.jd.blockchain.ledger.core;

import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Dataset;
import com.jd.blockchain.utils.Transactional;

public interface MerkleDataset extends Transactional, MerkleProvable, Dataset<Bytes, byte[]> {

	boolean isReadonly();

}