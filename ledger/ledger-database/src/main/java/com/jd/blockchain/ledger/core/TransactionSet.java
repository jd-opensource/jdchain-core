package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.utils.Bytes;

public interface TransactionSet extends MerkleProvable<Bytes> {

	LedgerTransaction[] getTransactions(int fromIndex, int count);
	
	TransactionResult[] getTransactionResults(int fromIndex, int count);

	long getTotalCount();

	/**
	 * @param txContentHash
	 *            Base58 编码的交易内容的哈希；
	 * @return
	 */
	LedgerTransaction getTransaction(HashDigest txContentHash);
	/**
	 * @param txContentHash
	 *            Base58 编码的交易内容的哈希；
	 * @return
	 */
	TransactionRequest getTransactionRequest(HashDigest txContentHash);
	/**
	 * @param txContentHash
	 *            Base58 编码的交易内容的哈希；
	 * @return
	 */
	TransactionResult getTransactionResult(HashDigest txContentHash);

	TransactionState getState(HashDigest txContentHash);

}