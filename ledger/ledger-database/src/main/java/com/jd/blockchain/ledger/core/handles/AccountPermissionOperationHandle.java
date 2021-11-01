package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.AccountDataPermission;
import com.jd.blockchain.ledger.AccountModeBits;
import com.jd.blockchain.ledger.AccountPermissionSetOperation;
import com.jd.blockchain.ledger.ContractDoesNotExistException;
import com.jd.blockchain.ledger.DataAccountDoesNotExistException;
import com.jd.blockchain.ledger.DataPermission;
import com.jd.blockchain.ledger.EventAccountDoesNotExistException;
import com.jd.blockchain.ledger.PermissionAccount;
import com.jd.blockchain.ledger.RoleDoesNotExistException;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import utils.StringUtils;

public class AccountPermissionOperationHandle extends AbstractLedgerOperationHandle<AccountPermissionSetOperation> {

    public AccountPermissionOperationHandle() {
        super(AccountPermissionSetOperation.class);
    }

    @Override
    protected void doProcess(AccountPermissionSetOperation op, LedgerTransactionContext transactionContext, TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {

        PermissionAccount account = null;
        // 查找账户
        switch (op.getAccountType()) {
            case DATA:
                account = transactionContext.getDataset().getDataAccountSet().getAccount(op.getAddress());
                if (null == account) {
                    throw new DataAccountDoesNotExistException(String.format("Data account doesn't exist! --[Address=%s]", op.getAddress()));
                }
                break;
            case EVENT:
                account = transactionContext.getEventSet().getEventAccountSet().getAccount(op.getAddress());
                if (null == account) {
                    throw new EventAccountDoesNotExistException(String.format("Event account doesn't exist! --[Address=%s]", op.getAddress()));
                }
                break;
            case CONTRACT:
                account = transactionContext.getDataset().getContractAccountSet().getAccount(op.getAddress());
                if (null == account) {
                    throw new ContractDoesNotExistException(String.format("Contract doesn't exist! --[Address=%s]", op.getAddress()));
                }
                break;
        }

        if (!StringUtils.isEmpty(op.getRole()) && !transactionContext.getDataset().getAdminDataset().getAdminSettings().getRolePrivileges().contains(op.getRole())) {
            throw new RoleDoesNotExistException(String.format("Role doesn't exist! --[Role=%s]", op.getRole()));
        }

        // 写权限校验
        SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
        securityPolicy.checkDataOwners(account.getPermission(), MultiIDsPolicy.AT_LEAST_ONE);

        // 更新权限信息
        DataPermission originPermission = account.getPermission();
        AccountModeBits modeBits = op.getMode() > -1 ? new AccountModeBits(op.getAccountType(), op.getMode()) : originPermission.getModeBits();
        String rols = !StringUtils.isEmpty(op.getRole()) ? op.getRole().toUpperCase() : originPermission.getRole();
        account.setPermission(new AccountDataPermission(modeBits, originPermission.getOwners(), rols));
    }
}
