package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.contract.LocalContractEventContext;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public abstract class AbtractContractEventSendOperationHandle implements OperationHandle {

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

        return doProcess(requestContext, contractOP, transactionContext, ledger, opHandleContext, manager, securityPolicy);
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
            throw new IllegalAccountStateException("Can not call contract[" + contract.getAddress() + "] in " + contract.getState() + " state.");
        }

        ContractLedgerQueryService ledgerQueryService = new ContractLedgerQueryService(ledger);
        LedgerMetadata_V2 metadata = (LedgerMetadata_V2) ledgerQueryService.getLedgerMetadata();

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
                request.getEndpoints().stream().map(s -> s.getIdentity()).collect(Collectors.toSet()));

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
