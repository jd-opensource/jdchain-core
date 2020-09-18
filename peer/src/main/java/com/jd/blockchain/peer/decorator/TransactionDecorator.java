package com.jd.blockchain.peer.decorator;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.OperationResultData;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.TypedValue;

/**
 * 交易包装类
 *         用于JSON序列化显示
 *
 * @author shaozhuguang
 *
 */
public class TransactionDecorator implements LedgerTransaction {

    private HashDigest transactionHash;

    private long blockHeight;

    private TransactionState executionState;

    private OperationResult[] operationResults;

	private LedgerDataSnapshot dataSnapshot;

    public TransactionDecorator(LedgerTransaction ledgerTransaction) {
        this.blockHeight = ledgerTransaction.getBlockHeight();
        this.dataSnapshot = ledgerTransaction.getDataSnapshot();
        this.executionState = ledgerTransaction.getExecutionState();
        this.transactionHash = ledgerTransaction.getTransactionHash();
        
        initOperationResults(ledgerTransaction.getOperationResults());
    }

    private void initOperationResults(OperationResult[] opResults) {
        if (opResults != null && opResults.length > 0) {
            operationResults = new OperationResult[opResults.length];
            for (int i = 0; i < opResults.length; i++) {
                OperationResult opResult = opResults[i];
                TypedValue value = TypedValue.wrap(opResult.getResult());
                operationResults[i] = new OperationResultData(opResult.getIndex(), value);
            }
        }
    }

    @Override
    public HashDigest getTransactionHash() {
    	return transactionHash;
    }

    @Override
    public long getBlockHeight() {
        return this.blockHeight;
    }
    
    @Override
    public LedgerDataSnapshot getDataSnapshot() {
    	return dataSnapshot;
    }

    @Override
    public TransactionState getExecutionState() {
        return this.executionState;
    }

    @Override
    public OperationResult[] getOperationResults() {
        return this.operationResults;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public void setExecutionState(TransactionState executionState) {
        this.executionState = executionState;
    }

    public void setOperationResults(OperationResult[] operationResults) {
        this.operationResults = operationResults;
    }
}
