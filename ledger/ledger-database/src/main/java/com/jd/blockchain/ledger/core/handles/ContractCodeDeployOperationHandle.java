package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.contract.ContractException;
import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractVersionConflictException;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.core.*;


public class ContractCodeDeployOperationHandle extends AbstractLedgerOperationHandle<ContractCodeDeployOperation> {

	/**
	 * 4 MB MaxSize of contract;
	 */
	public static final int MAX_SIZE_OF_CONTRACT = 4 * 1024 * 1024;

	private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

	public ContractCodeDeployOperationHandle() {
		super(ContractCodeDeployOperation.class);
	}

	@Override
	protected void doProcess(ContractCodeDeployOperation op, LedgerTransactionContext transactionContext,
			TransactionRequestExtension requestContext, LedgerQuery ledger,
			OperationHandleContext handleContext, EventManager manager) {


		// TODO: 请求者应该提供合约账户的公钥签名，以确保注册人对注册的地址和公钥具有合法的使用权；

		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(LedgerPermission.UPGRADE_CONTRACT, MultiIDsPolicy.AT_LEAST_ONE);

		// 操作账本；
		ContractCodeDeployOperation contractOP = op;

		// 校验合约内容
		byte[] chainCode = contractOP.getChainCode();

		if (chainCode == null || chainCode.length == 0) {
			throw new ContractException(String.format("Contract[%s] content is empty !!!",
					contractOP.getContractID().getAddress().toBase58()));
		}
		if (chainCode.length > MAX_SIZE_OF_CONTRACT) {
			throw new ContractException(String.format("Contract[%s] content is great than the max size[%s]!",
					contractOP.getContractID().getAddress().toBase58(), MAX_SIZE_OF_CONTRACT));
		}

		// 校验合约代码，不通过会抛出异常
		try {
			if (!CONTRACT_PROCESSOR.verify(chainCode)) {
				throw new ContractException(String.format("Contract[%s] verify fail !!!",
						contractOP.getContractID().getAddress().toBase58()));
			}
		} catch (Exception e) {
			throw new ContractException(String.format("Contract[%s] verify fail !!!",
					contractOP.getContractID().getAddress().toBase58()));
		}

		// chainCodeVersion != null? then use it;
		long contractVersion = contractOP.getChainCodeVersion();
		if(contractVersion != -1L){
			long rst = transactionContext.getDataset().getContractAccountSet().update(contractOP.getContractID().getAddress(),
					contractOP.getChainCode(), contractVersion);
			if(rst < 0 ){
				throw new ContractVersionConflictException();
			}
		} else {
			transactionContext.getDataset().getContractAccountSet().deploy(contractOP.getContractID().getAddress(),
					contractOP.getContractID().getPubKey(), contractOP.getAddressSignature(), contractOP.getChainCode());
		}
	}

}
