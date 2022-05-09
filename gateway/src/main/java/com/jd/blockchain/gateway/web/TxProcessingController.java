package com.jd.blockchain.gateway.web;

import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.gateway.service.LedgersManager;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.HashAlgorithmUpdateOperation;
import com.jd.blockchain.sdk.service.ErrorTransactionResponse;
import com.jd.blockchain.transaction.TxBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.gateway.service.LedgersService;
import com.jd.blockchain.transaction.SignatureUtils;
import com.jd.blockchain.transaction.TransactionService;
import com.jd.blockchain.web.converters.BinaryMessageConverter;

import utils.exception.ViewObsoleteException;

/**
 * @author huanghaiquan
 *
 */
@RestController
public class TxProcessingController implements TransactionService {

	private Logger LOGGER = LoggerFactory.getLogger(TxProcessingController.class);

	private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

	@Autowired
	private LedgersService peerService;

	@Autowired
	private LedgersManager peerConnector;

	@RequestMapping(path = "rpc/tx", method = RequestMethod.POST, consumes = BinaryMessageConverter.CONTENT_TYPE_VALUE, produces = BinaryMessageConverter.CONTENT_TYPE_VALUE)
	@Override
	public @ResponseBody TransactionResponse process(@RequestBody TransactionRequest txRequest) {
		HashDigest ledgerHash = null;
		try {
			LOGGER.info("receive transaction -> [contentHash={}, timestamp ={}]", txRequest.getTransactionHash(), txRequest.getTransactionContent().getTimestamp());

			TransactionContent txContent = txRequest.getTransactionContent();
			// 检查交易请求的信息是否完整；
			ledgerHash = txContent.getLedgerHash();
			if (ledgerHash == null) {
				// 未指定交易的账本；
				return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.LEDGER_HASH_EMPTY);
			}

			// 校验交易中部署合约的合法性，同时检验该交易是否包含更新账本配置环境的操作
			Operation[] operations = txContent.getOperations();
			boolean ledgerSettingUpdate = false;
			if (operations != null && operations.length > 0) {
				for (Operation op : operations) {
					if (ContractCodeDeployOperation.class.isAssignableFrom(op.getClass())) {
						// 发布合约请求
						ContractCodeDeployOperation opration = (ContractCodeDeployOperation) op;
						if ((null == opration.getLang() || opration.getLang().equals(ContractLang.Java)) && !CONTRACT_PROCESSOR.verify(opration.getChainCode())) {
							return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.ILLEGAL_CONTRACT_CAR);
						}
					} else if (HashAlgorithmUpdateOperation.class.isAssignableFrom(op.getClass())) {
						ledgerSettingUpdate = true;
					}
				}
			}

			// 预期的请求中不应该包含节点签名，首个节点签名应该由当前网关提供；
			if (txRequest.getNodeSignatures() != null && txRequest.getNodeSignatures().length > 0) {
				return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.ILLEGAL_NODE_SIGNATURE);
			}

			// 校验交易哈希
			if (!TxBuilder.verifyTxContentHash(txContent, txRequest.getTransactionHash())) {
				return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.INVALID_ENDPOINT_SIGNATURE);
			}

			// 终端签名校验
			DigitalSignature[] partiSigns = txRequest.getEndpointSignatures();
			if (partiSigns == null || partiSigns.length == 0) {
				return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.NO_ENDPOINT_SIGNATURE);
			} else {
				for (DigitalSignature sign : partiSigns) {
					if (!SignatureUtils.verifyHashSignature(txRequest.getTransactionHash(), sign.getDigest(), sign.getPubKey())) {
						return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.INVALID_ENDPOINT_SIGNATURE);
					}
				}
			}

			LOGGER.info("[contentHash={}],before peerService.getTransactionService().process(txRequest)", txRequest.getTransactionHash());
			TransactionResponse transactionResponse = peerService.getTransactionService(ledgerHash).process(txRequest);
			LOGGER.info("[contentHash={}],after peerService.getTransactionService().process(txRequest)", txRequest.getTransactionHash());

			// 如果属于账本环境更新的交易，应该触发网关对peer的重连操作以更新网关接入环境
			if (ledgerSettingUpdate) {
				peerConnector.reset(ledgerHash);
			}

			return transactionResponse;
		} catch (ViewObsoleteException voe) {
			peerConnector.reset(ledgerHash);
			return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.SYSTEM_ERROR);
		} catch (Exception e) {
			LOGGER.error("[contentHash="+ txRequest.getTransactionHash() +"] process error", e);
			return new ErrorTransactionResponse(txRequest.getTransactionHash(), TransactionState.SYSTEM_ERROR);
		}
	}
}
