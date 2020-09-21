//package com.jd.blockchain.peer.decorator;
//
//import com.jd.blockchain.crypto.HashDigest;
//import com.jd.blockchain.ledger.LedgerDataSnapshot;
//import com.jd.blockchain.ledger.LedgerTransaction;
//import com.jd.blockchain.ledger.OperationResult;
//import com.jd.blockchain.ledger.OperationResultData;
//import com.jd.blockchain.ledger.TransactionRequest;
//import com.jd.blockchain.ledger.TransactionResult;
//import com.jd.blockchain.ledger.TransactionState;
//import com.jd.blockchain.ledger.TypedValue;
//import com.jd.blockchain.ledger.core.TransactionResultData;
//
///**
// * 交易包装类 用于JSON序列化显示
// *
// * @author shaozhuguang
// *
// */
//public class TransactionDecorator implements LedgerTransaction {
//
//	private TransactionRequest request;
//
//	private TransactionResult result;
//
//	public TransactionDecorator(LedgerTransaction ledgerTransaction) {
//		this.request = ledgerTransaction.getRequest();
//		this.result = ledgerTransaction.getResult();
//
//		TransactionResult txResult = ledgerTransaction.getResult();
//		OperationResult[] opResults = initOperationResults(result.getOperationResults());
//
//		this.result = new TransactionResultData(txResult.getTransactionHash(), txResult.getBlockHeight(),
//				txResult.getExecutionState(), txResult.getDataSnapshot(), opResults);
//	}
//
//	private OperationResult[] initOperationResults(OperationResult[] opResults) {
//		OperationResult[] operationResults = null;
//		if (opResults != null && opResults.length > 0) {
//			operationResults = new OperationResult[opResults.length];
//			for (int i = 0; i < opResults.length; i++) {
//				OperationResult opResult = opResults[i];
//				TypedValue value = TypedValue.wrap(opResult.getResult());
//				operationResults[i] = new OperationResultData(opResult.getIndex(), value);
//			}
//		}
//
//		return operationResults;
//	}
//
//}
