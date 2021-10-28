package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.ledger.ContractDoesNotExistException;
import com.jd.blockchain.ledger.ContractStateUpdateOperation;
import com.jd.blockchain.ledger.IllegalAccountStateException;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.core.ContractAccount;
import com.jd.blockchain.ledger.core.ContractAccountSetEditor;
import com.jd.blockchain.ledger.core.ContractAccountSetEditorSimple;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

public class ContractStateUpdateOperationHandle extends AbstractLedgerOperationHandle<ContractStateUpdateOperation> {

    public ContractStateUpdateOperationHandle() {
        super(ContractStateUpdateOperation.class);
    }

    @Override
    protected void doProcess(ContractStateUpdateOperation op, LedgerTransactionContext transactionContext,
                             TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
        // 权限校验；
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkEndpointPermission(LedgerPermission.UPDATE_CONTRACT_STATE, MultiIDsPolicy.AT_LEAST_ONE);

        ContractAccount contract = transactionContext.getDataset().getContractAccountSet().getAccount(op.getContractAddress());

        if (null == contract) {
            throw new ContractDoesNotExistException(String.format("Contract doesn't exist! --[Address=%s]", op.getContractAddress()));
        }

        // REVOKE 状态不可再恢复
        if (contract.getState() == AccountState.REVOKE) {
            throw new IllegalAccountStateException(String.format("Can not change contract in REVOKE state! --[Address=%s]", op.getContractAddress()));
        }

        // 操作账本；
        if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            ((ContractAccountSetEditor) (transactionContext.getDataset().getContractAccountSet())).setState(op.getContractAddress(), op.getState());
        } else {
            ((ContractAccountSetEditorSimple) (transactionContext.getDataset().getContractAccountSet())).setState(op.getContractAddress(), op.getState());
        }
    }

}
