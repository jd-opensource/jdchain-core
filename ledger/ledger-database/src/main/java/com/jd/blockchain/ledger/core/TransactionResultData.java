package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.TransactionState;

public class TransactionResultData implements TransactionResult {

	private HashDigest transactionHash;
	private long blockHeight;
	private TransactionState txExecState;
	private OperationResult[] operationResults;
	private LedgerDataSnapshot dataSnapshot;
	private Operation[] derivedOperations;

	public TransactionResultData(HashDigest transactionHash, long blockHeight, TransactionState txExecState,
			LedgerDataSnapshot dataSnapshot, OperationResult... operationResults) {
		this(transactionHash, blockHeight, txExecState, dataSnapshot, operationResults, null);
	}

	public TransactionResultData(HashDigest transactionHash, long blockHeight, TransactionState txExecState,
								 LedgerDataSnapshot dataSnapshot, OperationResult[] operationResults, Operation[] derivedOperations) {
		this.transactionHash = transactionHash;
		this.blockHeight = blockHeight;
		this.txExecState = txExecState;
		this.dataSnapshot = dataSnapshot;
		this.operationResults = operationResults;
		this.derivedOperations = derivedOperations;
	}
	
	@Override
	public HashDigest getTransactionHash() {
		return transactionHash;
	}

	@Override
	public long getBlockHeight() {
		return blockHeight;
	}

	@Override
	public TransactionState getExecutionState() {
		return txExecState;
	}

	@Override
	public OperationResult[] getOperationResults() {
		return operationResults;
	}

	@Override
	public LedgerDataSnapshot getDataSnapshot() {
		return dataSnapshot;
	}

	@Override
	public Operation[] getDerivedOperations() {
		return derivedOperations;
	}

}
