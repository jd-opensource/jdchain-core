package com.jd.blockchain.peer.decorator;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.transaction.DigitalSignatureBlob;
import com.jd.blockchain.transaction.TxContentBlob;

/**
 * 交易包装类
 *         用于JSON序列化显示
 *
 * @author shaozhuguang
 *
 */
public class TransactionDecorator implements LedgerTransaction {

    private HashDigest adminAccountHash;

    private HashDigest userAccountSetHash;

    private HashDigest dataAccountSetHash;

    private HashDigest contractAccountSetHash;

    private HashDigest hash;

    private long blockHeight;

    private TransactionState executionState;

    private TransactionContent transactionContent;

    private DigitalSignature[] endpointSignatures;

    private DigitalSignature[] nodeSignatures;

    private OperationResult[] operationResults;

    public TransactionDecorator(LedgerTransaction ledgerTransaction) {
        this.hash = ledgerTransaction.getHash();
        this.blockHeight = ledgerTransaction.getBlockHeight();
        this.adminAccountHash = ledgerTransaction.getAdminAccountHash();
        this.userAccountSetHash = ledgerTransaction.getUserAccountSetHash();
        this.dataAccountSetHash = ledgerTransaction.getDataAccountSetHash();
        this.contractAccountSetHash = ledgerTransaction.getContractAccountSetHash();
        this.executionState = ledgerTransaction.getExecutionState();

        initTxContent(ledgerTransaction.getTransactionContent());
        initEndpointSignatures(ledgerTransaction.getEndpointSignatures());
        initNodeSignatures(ledgerTransaction.getNodeSignatures());
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

    private void initNodeSignatures(DigitalSignature[] nodeSigns) {
        if (nodeSigns != null && nodeSigns.length > 0) {
            this.nodeSignatures = new DigitalSignature[nodeSigns.length];
            for (int i = 0; i < nodeSigns.length; i++) {
                this.nodeSignatures[i] = new DigitalSignatureBlob(
                        nodeSigns[i].getPubKey(), nodeSigns[i].getDigest());
            }
        }
    }

    private void initEndpointSignatures(DigitalSignature[] endpointSigns) {
        if (endpointSigns != null && endpointSigns.length > 0) {
            this.endpointSignatures = new DigitalSignature[endpointSigns.length];
            for (int i = 0; i < endpointSigns.length; i++) {
                this.endpointSignatures[i] = new DigitalSignatureBlob(
                        endpointSigns[i].getPubKey(), endpointSigns[i].getDigest());
            }
        }
    }

    private void initTxContent(TransactionContent txContent) {
        TxContentBlob txContentBlob = new TxContentBlob(txContent.getLedgerHash());
        txContentBlob.setHash(txContent.getHash());
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
        this.transactionContent = txContentBlob;
    }

    private Operation initOperation(Operation op) {
        return OperationDecoratorFactory.decorate(op);
    }

    @Override
    public HashDigest getAdminAccountHash() {
        return this.adminAccountHash;
    }

    @Override
    public HashDigest getUserAccountSetHash() {
        return this.userAccountSetHash;
    }

    @Override
    public HashDigest getDataAccountSetHash() {
        return this.dataAccountSetHash;
    }

    @Override
    public HashDigest getContractAccountSetHash() {
        return this.contractAccountSetHash;
    }

    @Override
    public HashDigest getHash() {
        return this.hash;
    }

    @Override
    public TransactionContent getTransactionContent() {
        return this.transactionContent;
    }

    @Override
    public DigitalSignature[] getEndpointSignatures() {
        return this.endpointSignatures;
    }

    @Override
    public long getBlockHeight() {
        return this.blockHeight;
    }

    @Override
    public TransactionState getExecutionState() {
        return this.executionState;
    }

    @Override
    public OperationResult[] getOperationResults() {
        return this.operationResults;
    }

    @Override
    public DigitalSignature[] getNodeSignatures() {
        return this.nodeSignatures;
    }

    public void setAdminAccountHash(HashDigest adminAccountHash) {
        this.adminAccountHash = adminAccountHash;
    }

    public void setUserAccountSetHash(HashDigest userAccountSetHash) {
        this.userAccountSetHash = userAccountSetHash;
    }

    public void setDataAccountSetHash(HashDigest dataAccountSetHash) {
        this.dataAccountSetHash = dataAccountSetHash;
    }

    public void setContractAccountSetHash(HashDigest contractAccountSetHash) {
        this.contractAccountSetHash = contractAccountSetHash;
    }

    public void setHash(HashDigest hash) {
        this.hash = hash;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public void setTransactionContent(TransactionContent transactionContent) {
        this.transactionContent = transactionContent;
    }

    public void setEndpointSignatures(DigitalSignature[] endpointSignatures) {
        this.endpointSignatures = endpointSignatures;
    }

    public void setNodeSignatures(DigitalSignature[] nodeSignatures) {
        this.nodeSignatures = nodeSignatures;
    }

    public void setExecutionState(TransactionState executionState) {
        this.executionState = executionState;
    }

    public void setOperationResults(OperationResult[] operationResults) {
        this.operationResults = operationResults;
    }
}
