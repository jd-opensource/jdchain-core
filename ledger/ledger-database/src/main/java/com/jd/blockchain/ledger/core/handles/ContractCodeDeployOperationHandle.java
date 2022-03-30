package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.contract.ContractException;
import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import utils.Bytes;


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

		// 校验合约内容
		byte[] chainCode = op.getChainCode();

		if (chainCode == null || chainCode.length == 0) {
			throw new ContractException(String.format("Contract[%s] content is empty !!!", op.getContractID().getAddress().toBase58()));
		}
		if (chainCode.length > MAX_SIZE_OF_CONTRACT) {
			throw new ContractException(String.format("Contract[%s] content is great than the max size[%s]!", op.getContractID().getAddress().toBase58(), MAX_SIZE_OF_CONTRACT));
		}

		// 校验合约代码，不通过会抛出异常
		try {
			if ((null == op.getLang() || op.getLang().equals(ContractLang.Java)) && !CONTRACT_PROCESSOR.verify(chainCode)) {
				throw new ContractException(String.format("Contract[%s] verify fail !!!", op.getContractID().getAddress().toBase58()));
			}
		} catch (Exception e) {
			throw new ContractException(String.format("Contract[%s] verify fail !!!", op.getContractID().getAddress().toBase58()));
		}

		// 校验合约状态
		ContractAccount contract = transactionContext.getDataset().getContractAccountSet().getAccount(op.getContractID().getAddress());
		if (null != contract && contract.getState() != AccountState.NORMAL) {
			throw new IllegalTransactionException("Can not change contract[" + contract.getAddress() + "] in "+ contract.getState() +" state.");
		}

		// chainCodeVersion != null? then use it;
		long contractVersion = op.getChainCodeVersion();
		if(contractVersion != -1L){
			long rst = 0;

			rst = ((ContractAccountSetEditor)(transactionContext.getDataset().getContractAccountSet())).update(op.getContractID().getAddress(),
					op.getChainCode(), contractVersion, op.getLang());

			if(rst < 0 ){
				throw new ContractVersionConflictException();
			}
		} else {
			ContractAccount account = ((ContractAccountSetEditor)(transactionContext.getDataset().getContractAccountSet())).deploy(op.getContractID().getAddress(),
				op.getContractID().getPubKey(), op.getAddressSignature(), op.getChainCode(), op.getLang());

			account.setPermission(new AccountDataPermission(AccountType.CONTRACT, requestContext.getEndpointAddresses().toArray(new Bytes[0])));
		}
	}

}
