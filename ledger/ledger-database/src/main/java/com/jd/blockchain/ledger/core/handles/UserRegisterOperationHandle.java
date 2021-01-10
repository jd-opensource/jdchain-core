package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.LedgerDataSetEditor;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;

import utils.Bytes;

import com.jd.blockchain.ledger.core.EventManager;

public class UserRegisterOperationHandle extends AbstractLedgerOperationHandle<UserRegisterOperation> {

	public UserRegisterOperationHandle() {
		super(UserRegisterOperation.class);
	}

	@Override
	protected void doProcess(UserRegisterOperation op, LedgerDataSetEditor newBlockDataset,
							 TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_USER, MultiIDsPolicy.AT_LEAST_ONE);

		// 操作账本；
		UserRegisterOperation userRegOp = (UserRegisterOperation) op;
		BlockchainIdentity bid = userRegOp.getUserID();

		Bytes userAddress = bid.getAddress();

		newBlockDataset.getUserAccountSet().register(userAddress, bid.getPubKey());
	}

}
