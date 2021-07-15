package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.DataAccountDoesNotExistException;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountKVSetOperation.KVWriteEntry;
import com.jd.blockchain.ledger.DataPermissionType;
import com.jd.blockchain.ledger.DataVersionConflictException;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.core.DataAccount;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import com.jd.blockchain.ledger.core.EventManager;

public class DataAccountKVSetOperationHandle extends AbstractLedgerOperationHandle<DataAccountKVSetOperation> {

	public DataAccountKVSetOperationHandle() {
		super(DataAccountKVSetOperation.class);
	}

	@Override
	protected void doProcess(DataAccountKVSetOperation kvWriteOp, LedgerTransactionContext transactionContext,
			TransactionRequestExtension requestContext, LedgerQuery ledger, 
			OperationHandleContext handleContext, EventManager manager) {
		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(LedgerPermission.WRITE_DATA_ACCOUNT, MultiIDsPolicy.AT_LEAST_ONE);

		// 操作账本；
		DataAccount account = transactionContext.getDataset().getDataAccountSet().getAccount(kvWriteOp.getAccountAddress());
		if (account == null) {
			throw new DataAccountDoesNotExistException("DataAccount doesn't exist!");
		}

		// 写权限校验
		securityPolicy.checkDataPermission(account.getPermission(), DataPermissionType.WRITE);

		KVWriteEntry[] writeSet = kvWriteOp.getWriteSet();
		long v = -1L;
		for (KVWriteEntry kvw : writeSet) {
			v = account.getDataset().setValue(kvw.getKey(), TypedValue.wrap(kvw.getValue()), kvw.getExpectedVersion());
			if (v < 0) {
				throw new DataVersionConflictException();
			}
		}
	}

}
