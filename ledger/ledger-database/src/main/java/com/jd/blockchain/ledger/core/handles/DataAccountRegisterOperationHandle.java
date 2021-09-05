package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.core.DataAccountSetEditor;
import com.jd.blockchain.ledger.core.DataAccountSetEditorSimple;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jd.blockchain.ledger.core.EventManager;

public class DataAccountRegisterOperationHandle extends AbstractLedgerOperationHandle<DataAccountRegisterOperation> {
	private Logger logger = LoggerFactory.getLogger(DataAccountRegisterOperationHandle.class);

	public DataAccountRegisterOperationHandle() {
		super(DataAccountRegisterOperation.class);
	}

	@Override
	protected void doProcess(DataAccountRegisterOperation op, LedgerTransactionContext transactionContext,
			TransactionRequestExtension requestContext, LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager) {
		// TODO: 请求者应该提供数据账户的公钥签名，以更好地确保注册人对该地址和公钥具有合法使用权；

		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(LedgerPermission.REGISTER_DATA_ACCOUNT, MultiIDsPolicy.AT_LEAST_ONE);

		// 操作账本；
		DataAccountRegisterOperation dataAccountRegOp = (DataAccountRegisterOperation) op;
		BlockchainIdentity bid = dataAccountRegOp.getAccountID();
		logger.debug("before register.[dataAddress={}]",bid.getAddress());

		if (ledger.getAnchorType().equals("default")) {
			((DataAccountSetEditor)(transactionContext.getDataset().getDataAccountSet())).register(bid.getAddress(), bid.getPubKey(), null);
		} else {
			((DataAccountSetEditorSimple)(transactionContext.getDataset().getDataAccountSet())).register(bid.getAddress(), bid.getPubKey(), null);
		}

		logger.debug("after register.[dataAddress={}]",bid.getAddress());
	}

}
