package com.jd.blockchain.peer.ledger.service.utils;

import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.OperationResultData;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.core.TransactionResultData;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;
import com.jd.blockchain.transaction.DigitalSignatureBlob;
import com.jd.blockchain.transaction.TxContentBlob;
import com.jd.blockchain.transaction.TxRequestMessage;

/**
 * 交易包装类 用于JSON序列化显示
 *
 * @author shaozhuguang
 *
 */
public class TransactionDecorator implements LedgerTransaction {

	private TransactionRequest request;

	private TransactionResult result;

	public TransactionDecorator(LedgerTransaction ledgerTransaction) {
        TransactionRequest request = ledgerTransaction.getRequest();
        TransactionResult result = ledgerTransaction.getResult();
        TransactionContent transactionContent = initTxContent(request.getTransactionContent());
        LedgerDataSnapshot ledgerDataSnapshot = initTransactionStagedSnapshot(result.getDataSnapshot());
        OperationResult[] operationResults = initOperationResults(result.getOperationResults());

		this.request = new TxRequestMessage(request.getTransactionHash(), transactionContent);
		this.result = new TransactionResultData(request.getTransactionHash(), result.getBlockHeight(), result.getExecutionState(),
                ledgerDataSnapshot, operationResults, initDerivedOperations(result.getDerivedOperations()));
        ((TxRequestMessage)this.request).setNodeSignatures(initNodeSignatures(request.getNodeSignatures()));
        ((TxRequestMessage)this.request).setEndpointSignatures(initEndpointSignatures(request.getEndpointSignatures()));
	}

	private OperationResult[] initOperationResults(OperationResult[] opResults) {
		OperationResult[] operationResults = null;
		if (opResults != null && opResults.length > 0) {
			operationResults = new OperationResult[opResults.length];
			for (int i = 0; i < opResults.length; i++) {
				OperationResult opResult = opResults[i];
				TypedValue value = TypedValue.wrap(opResult.getResult());
				operationResults[i] = new OperationResultData(opResult.getIndex(), value);
			}
		}

		return operationResults;
	}

    private TransactionContent initTxContent(TransactionContent txContent) {
        TxContentBlob txContentBlob = new TxContentBlob(txContent.getLedgerHash());
        txContentBlob.setTime(txContent.getTimestamp());
        Operation[] operations = txContent.getOperations();
        if (operations != null && operations.length > 0) {
            for (Operation op : operations) {
                Operation opDecorator = initOperation(op);
                if (opDecorator != null) {
                    txContentBlob.addOperation(opDecorator);
                }
            }
        }
        return txContentBlob;
    }

    private Operation[] initDerivedOperations(Operation[] operations) {
	    if(null == operations || operations.length == 0) {
            return null;
        }
        Operation[] ops = new Operation[operations.length];
        for (int i=0; i<operations.length; i++) {
            Operation opDecorator = initOperation(operations[i]);
            if (opDecorator != null) {
                ops[i] = opDecorator;
            }
        }

        return ops;
    }

    private DigitalSignature[] initNodeSignatures(DigitalSignature[] nodeSigns) {
        DigitalSignature[] nodeSignatures = null;
        if (nodeSigns != null && nodeSigns.length > 0) {
            nodeSignatures = new DigitalSignature[nodeSigns.length];
            for (int i = 0; i < nodeSigns.length; i++) {
                nodeSignatures[i] = new DigitalSignatureBlob(
                        nodeSigns[i].getPubKey(), nodeSigns[i].getDigest());
            }
        }
        return nodeSignatures;
    }

    private DigitalSignature[] initEndpointSignatures(DigitalSignature[] endpointSigns) {
        DigitalSignature[] endpointSignatures = null;
        if (endpointSigns != null && endpointSigns.length > 0) {
            endpointSignatures = new DigitalSignature[endpointSigns.length];
            for (int i = 0; i < endpointSigns.length; i++) {
                endpointSignatures[i] = new DigitalSignatureBlob(
                        endpointSigns[i].getPubKey(), endpointSigns[i].getDigest());
            }
        }
        return endpointSignatures;
    }

    private TransactionStagedSnapshot initTransactionStagedSnapshot(LedgerDataSnapshot ledgerDataSnapshot) {
        TransactionStagedSnapshot transactionStagedSnapshot = new TransactionStagedSnapshot();

        transactionStagedSnapshot.setAdminAccountHash(ledgerDataSnapshot.getAdminAccountHash());
        transactionStagedSnapshot.setUserAccountSetHash(ledgerDataSnapshot.getUserAccountSetHash());
        transactionStagedSnapshot.setContractAccountSetHash(ledgerDataSnapshot.getContractAccountSetHash());
        transactionStagedSnapshot.setDataAccountSetHash(ledgerDataSnapshot.getDataAccountSetHash());
        transactionStagedSnapshot.setSystemEventSetHash(ledgerDataSnapshot.getSystemEventSetHash());
        transactionStagedSnapshot.setUserEventSetHash(ledgerDataSnapshot.getUserEventSetHash());

        return transactionStagedSnapshot;
    }

    private Operation initOperation(Operation op) {
        return OperationDecoratorFactory.decorate(op);
    }

    @Override
    public TransactionRequest getRequest() {
        return request;
    }

    @Override
    public TransactionResult getResult() {
        return result;
    }
}
