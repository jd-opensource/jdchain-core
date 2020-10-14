package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResult;

public class LedgerTransactionData implements LedgerTransaction {
	
	private TransactionRequest txReq;
	
	private TransactionResult txResult;
	


	/**
	 * Declare a private no-arguments constructor for deserializing purpose；
	 */
	@SuppressWarnings("unused")
	private LedgerTransactionData() {
	}

	/**
	 * @param blockHeight 区块链高度；
	 * @param txReq       交易请求；
	 * @param execState   执行状态；
	 * @param txSnapshot  交易级的系统快照；
	 */
	public LedgerTransactionData(TransactionRequest txReq, TransactionResult txResult) {
		this.txReq = txReq;
		this.txResult = txResult;
	}

	@Override
	public TransactionRequest getRequest() {
		return txReq;
	}

	@Override
	public TransactionResult getResult() {
		return txResult;
	}

	
}
