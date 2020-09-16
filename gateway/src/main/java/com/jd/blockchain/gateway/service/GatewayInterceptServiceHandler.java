package com.jd.blockchain.gateway.service;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.TransactionRequest;


@Service
public class GatewayInterceptServiceHandler implements GatewayInterceptService {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GatewayInterceptServiceHandler.class);

    private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

    @Override
    public void intercept(HttpServletRequest request, TransactionRequest txRequest) {
        LOGGER.info("TxRequest[{}:{}] -> [{} -> {}]", request.getRemoteAddr(), request.getRemotePort(),
                txRequest.getTransactionHash().toBase58(), txRequest.getTransactionHash().toBase58());
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
