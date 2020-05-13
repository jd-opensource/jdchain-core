package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.TransactionState;

public interface TransactionQuery extends TxMerkleProvable {

	LedgerTransaction[] getTxs(int fromIndex, int count);

	LedgerTransaction[] getBlockTxs(int fromIndex, int count, HashDigest baseRootHash, HashDigest origRootHash);

	byte[][] getValuesByIndex(int fromIndex, int count);

	byte[][] getValuesByDiff(int fromIndex, int count, HashDigest baseRootHash, HashDigest origRootHash);

	long getTotalCount();

	/**
	 * @param txContentHash
	 *            Base58 编码的交易内容的哈希；
	 * @return
	 */
	LedgerTransaction get(HashDigest txContentHash);

	TransactionState getState(HashDigest txContentHash);

}