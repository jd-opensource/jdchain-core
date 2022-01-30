package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.contract.jvm.JVMContractRuntimeConfig;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.MultiLedgerQueryService;
import org.springframework.stereotype.Service;

import com.jd.blockchain.contract.LocalContractEventContext;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.core.ContractAccount;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandle;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.core.SecurityContext;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import com.jd.blockchain.ledger.core.EventManager;

import java.util.Stack;
import java.util.stream.Collectors;

@Service
public abstract class AbtractContractEventSendOperationHandle implements OperationHandle {

	// 保存合约调用栈信息
	private ThreadLocal<Stack<String>> contractEventStackTL = new ThreadLocal<>();
	// 合约调用栈最大深度
	private static final int MAX_CONTRACT_EVENT_STACK_SIZE = 100;

	@Override
	public Class<?> getOperationType() {
		return ContractEventSendOperation.class;
	}

	@Override
	public BytesValue process(Operation op, LedgerTransactionContext transactionContext, TransactionRequestExtension requestContext,
							  LedgerQuery ledger, OperationHandleContext opHandleContext, EventManager manager) {
		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(TransactionPermission.CONTRACT_OPERATION, MultiIDsPolicy.AT_LEAST_ONE);

		ContractEventSendOperation contractOP = (ContractEventSendOperation) op;

		// 处理合约调用栈
		Stack<String> contractEventStack = contractEventStackTL.get();
		if(null == contractEventStack) {
			contractEventStack = new Stack<>();
			contractEventStackTL.set(contractEventStack);
		}
		// 合约调用入栈
		contractEventStack.push(contractOP.getContractAddress() + contractOP.getEvent());
		try {
			// 合约调用栈最大深度检查，超过则回滚交易
			if(contractEventStack.size() > MAX_CONTRACT_EVENT_STACK_SIZE) {
				throw new ContractExecuteException(String.format("Size of contract event stack is greater than %d", MAX_CONTRACT_EVENT_STACK_SIZE));
			}
			return doProcess(requestContext, contractOP, transactionContext, ledger, opHandleContext, manager, securityPolicy);
		} finally {
			// 合约调用出栈
			contractEventStack.pop();
		}
	}

	private BytesValue doProcess(TransactionRequestExtension request, ContractEventSendOperation contractOP,
								 LedgerTransactionContext transactionContext, LedgerQuery ledger, OperationHandleContext opHandleContext,
								 EventManager manager, SecurityPolicy securityPolicy) {
		// 先从账本校验合约的有效性；
		// 注意：必须在前一个区块的数据集中进行校验，因为那是经过共识的数据；从当前新区块链数据集校验则会带来攻击风险：未经共识的合约得到执行；
		ContractAccount contract = ledger.getContractAccountset().getAccount(contractOP.getContractAddress());
		if (null == contract) {
			throw new ContractDoesNotExistException(String.format("Contract doesn't exist! --[Address=%s]", contractOP.getContractAddress()));
		}
		// 校验合约状态
		if (contract.getState() != AccountState.NORMAL) {
			throw new IllegalAccountStateException("Can not call contract[" + contract.getAddress() + "] in "+ contract.getState() +" state.");
		}

		ContractLedgerQueryService ledgerQueryService = new ContractLedgerQueryService(ledger);
		LedgerMetadata_V2 metadata = (LedgerMetadata_V2)ledgerQueryService.getLedgerMetadata();

		// 创建合约的账本上下文实例；
		ContractLedgerContext ledgerContext = new ContractLedgerContext(opHandleContext, ledgerQueryService, new MultiLedgerQueryService(ledger));
		UncommittedLedgerQueryService uncommittedLedgerQueryService = new UncommittedLedgerQueryService(transactionContext);

		// 执行权限校验
		securityPolicy.checkDataPermission(contract.getPermission(), DataPermissionType.EXECUTE);

		// 创建合约上下文;
		LocalContractEventContext localContractEventContext = new LocalContractEventContext(
				request.getTransactionContent().getLedgerHash(),
				metadata.getContractRuntimeConfig(),
				contract.getAddress(),
				contractOP.getEvent());
		localContractEventContext.setArgs(contractOP.getArgs()).setTransactionRequest(request)
				.setLedgerContext(ledgerContext).setVersion(contract.getChainCodeVersion())
				.setUncommittedLedgerContext(uncommittedLedgerQueryService);

		localContractEventContext.setTxSigners(
				request.getEndpoints().stream().map( s -> s.getIdentity()).collect(Collectors.toSet()));

		// 装载合约；
		ContractCode contractCode = loadContractCode(contract);

		// 处理合约事件；
		BytesValue result = contractCode.processEvent(localContractEventContext);
		// 交易上下文添加衍生操作
		transactionContext.addDerivedOperations(ledgerContext.getDerivedOperations());

		return result;
	}

	protected abstract ContractCode loadContractCode(ContractAccount contract);

}
