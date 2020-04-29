package com.jd.blockchain.gateway.service;

import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.TransactionRequest;
import org.springframework.stereotype.Service;


@Service
public class GatewayInterceptServiceHandler implements GatewayInterceptService {

    private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

    @Override
    public void intercept(TransactionRequest txRequest) {
        // 当前仅处理合约发布的请求
        Operation[] operations = txRequest.getTransactionContent().getOperations();
        if (operations != null && operations.length > 0) {
            for (Operation op : operations) {
                if (ContractCodeDeployOperation.class.isAssignableFrom(op.getClass())) {
                    // 发布合约请求
                    contractCheck((ContractCodeDeployOperation)op);
                }
            }
        }
    }

    private void contractCheck(final ContractCodeDeployOperation contractOP) {
        // 校验合约代码，不通过会抛出异常
        try {
            if (!CONTRACT_PROCESSOR.verify(contractOP.getChainCode())) {
                throw new IllegalStateException(String.format("Contract[%s] verify fail !!!",
                        contractOP.getContractID().getAddress().toBase58()));
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Contract[%s] verify fail !!!",
                    contractOP.getContractID().getAddress().toBase58()));
        }
    }
}
