package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.ConsensusReconfigOperation;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

/**
 * @Author: zhangshuang
 * @Date: 2021/12/15 2:43 PM
 * Version 1.0
 */
public class ConsensusReconfigOperationHandle  extends AbstractLedgerOperationHandle<ConsensusReconfigOperation>{

    public ConsensusReconfigOperationHandle() {
        super(ConsensusReconfigOperation.class);
    }

    @Override
    protected void doProcess(ConsensusReconfigOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery previousBlockDataset,
                             OperationHandleContext handleContext, EventManager manager) {

    }
}
