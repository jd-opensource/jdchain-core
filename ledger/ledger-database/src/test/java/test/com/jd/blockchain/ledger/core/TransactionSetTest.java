package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import com.jd.blockchain.ledger.ConsensusReconfigOperation;
import com.jd.blockchain.ledger.HashAlgorithmUpdateOperation;
import com.jd.blockchain.ledger.LedgerDataStructure;
import org.junit.Test;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainKeyGenerator;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.BytesDataList;
import com.jd.blockchain.ledger.ConsensusSettingsUpdateOperation;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.ParticipantRegisterOperation;
import com.jd.blockchain.ledger.ParticipantStateUpdateOperation;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionRequestBuilder;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.TransactionResultData;
import com.jd.blockchain.ledger.core.TransactionSetEditor;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;
import com.jd.blockchain.storage.service.utils.BufferedKVStorage;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.transaction.TxBuilder;

import utils.ArrayUtils;
import utils.codec.Base58Utils;

public class TransactionSetTest {

	private static final String keyPrefix = "L:/3A3dP4";

	private Random rand = new Random();

	static {
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(DataAccountRegisterOperation.class);
		DataContractRegistry.register(DataAccountKVSetOperation.class);
		DataContractRegistry.register(ContractCodeDeployOperation.class);
		DataContractRegistry.register(ContractEventSendOperation.class);
		DataContractRegistry.register(ParticipantRegisterOperation.class);
		DataContractRegistry.register(ParticipantStateUpdateOperation.class);
		DataContractRegistry.register(ConsensusSettingsUpdateOperation.class);
		DataContractRegistry.register(ConsensusReconfigOperation.class);
		DataContractRegistry.register(HashAlgorithmUpdateOperation.class);
	}

	@Test
	public void testSingleTransactionGetAndSet() {
		CryptoSetting cryptoSetting = LedgerTestUtils.createDefaultCryptoSetting();

		MemoryKVStorage testStorage = new MemoryKVStorage();

		// Create a new TransactionSet, it's empty;
		TransactionSetEditor txset = new TransactionSetEditor(cryptoSetting, keyPrefix, testStorage, testStorage, LedgerDataStructure.MERKLE_TREE);
		assertTrue(txset.isUpdated());
		assertFalse(txset.isReadonly());
		assertNull(txset.getRootHash());

		HashDigest ledgerHash = LedgerTestUtils.generateRandomHash();
		TransactionRequest txReq = buildTransactionRequest_RandomOperation(ledgerHash, cryptoSetting);

		long blockHeight = 8922L;
		TransactionState txState = TransactionState.SUCCESS;
		TransactionResult tx = buildTransactionResult(txReq, blockHeight, txState);

		txset.addTransaction(txReq, tx);

		assertTrue(txset.isUpdated());

		txset.commit();

		HashDigest txsetRootHash = txset.getRootHash();
		assertNotNull(txsetRootHash);
		assertEquals(1, txset.getTotalCount());
		assertEquals(blockHeight, tx.getBlockHeight());
		assertEquals(ledgerHash, txReq.getTransactionContent().getLedgerHash());

		// Reload ;
		TransactionSetEditor reloadTxset = new TransactionSetEditor(-1,  txsetRootHash, cryptoSetting, keyPrefix, testStorage,
				testStorage, LedgerDataStructure.MERKLE_TREE, true);

		assertEquals(1, reloadTxset.getTotalCount());

		TransactionResult reloadTx = reloadTxset.getTransactionResult(txReq.getTransactionHash());

		assertNotNull(reloadTx);
		assertEquals(txState, reloadTx.getExecutionState());

		TransactionState state = reloadTxset.getState(txReq.getTransactionHash());
		assertEquals(txState, state);

		assertTransactionEquals(tx, reloadTx);
	}

	@Test
	public void testTransactionSequence() {
		CryptoSetting cryptoSetting = LedgerTestUtils.createDefaultCryptoSetting();
		MemoryKVStorage testStorage = new MemoryKVStorage();
		// Create a new TransactionSet;
		TransactionSetEditor txset = new TransactionSetEditor(cryptoSetting, keyPrefix, testStorage, testStorage, LedgerDataStructure.MERKLE_TREE);

		HashDigest ledgerHash = LedgerTestUtils.generateRandomHash();
		long blockHeight = 8922L;

		// 生成指定数量的测试数据：交易请求和交易执行结果；
		int txCount0 = 10;
		TransactionRequest[] txRequests_0 = new TransactionRequest[txCount0];
		TransactionResult[] txResults_0 = new TransactionResult[txCount0];
		buildRequestAndResult(ledgerHash, blockHeight, cryptoSetting, txCount0, txRequests_0, txResults_0);

		// add tx to trasaction set;
		for (int i = 0; i < txCount0; i++) {
			txset.addTransaction(txRequests_0[i], txResults_0[i]);
		}
		txset.commit();

		// 验证交易集合中记录的交易顺序；
		assertEquals(txCount0, txset.getTotalCount());
		TransactionResult[] actualTxResults = txset.getTransactionResults(0, txCount0);
		assertEquals(txCount0, actualTxResults.length);
		for (int i = 0; i < txCount0; i++) {
			assertTransactionEquals(txResults_0[i], actualTxResults[i]);
		}

		// 重新加载交易集合；
		HashDigest txsetRootHash = txset.getRootHash();
		TransactionSetEditor reloadTxset = new TransactionSetEditor(-1, txsetRootHash, cryptoSetting, keyPrefix, testStorage,
				testStorage, LedgerDataStructure.MERKLE_TREE, true);

		// 验证重新加载之后的交易集合中记录的交易顺序；
		assertEquals(txCount0, reloadTxset.getTotalCount());
		TransactionResult[] actualTxResults_reload = reloadTxset.getTransactionResults(0, txCount0);
		assertEquals(txCount0, actualTxResults_reload.length);
		for (int i = 0; i < txCount0; i++) {
			assertTransactionEquals(txResults_0[i], actualTxResults_reload[i]);
		}

		// 生成指定数量的测试数据：交易请求和交易执行结果；
		int txCount1 = new Random().nextInt(200) + 1;
		TransactionRequest[] txRequests_1 = new TransactionRequest[txCount1];
		TransactionResult[] txResults_1 = new TransactionResult[txCount1];
		buildRequestAndResult(ledgerHash, blockHeight, cryptoSetting, txCount1, txRequests_1, txResults_1);

		// add tx to trasaction set;
		TransactionSetEditor newTxset = new TransactionSetEditor(-1, txsetRootHash, cryptoSetting, keyPrefix, testStorage, testStorage, LedgerDataStructure.MERKLE_TREE,
				false);
		for (int i = 0; i < txCount1; i++) {
			newTxset.addTransaction(txRequests_1[i], txResults_1[i]);
		}
		newTxset.commit();

		// 验证交易集合中记录的交易顺序；
		int totalCount = txCount0 + txCount1;
		assertEquals(totalCount, newTxset.getTotalCount());
		TransactionResult[] actualTxResults_reload_2 = newTxset.getTransactionResults(0, totalCount);
		assertEquals(totalCount, actualTxResults_reload_2.length);

		TransactionRequest[] txRequests = ArrayUtils.concat(txRequests_0, txRequests_1, TransactionRequest.class);
		TransactionResult[] txResults = ArrayUtils.concat(txResults_0, txResults_1, TransactionResult.class);
		for (int i = 0; i < totalCount; i++) {
			assertTransactionEquals(txResults[i], actualTxResults_reload_2[i]);
		}
	}

	private void buildRequestAndResult(HashDigest ledgerHash, long blockHeight, CryptoSetting cryptoSetting,
			int txCount, TransactionRequest[] txRequests, TransactionResult[] txResults) {
		TransactionState[] TX_EXEC_STATES = TransactionState.values();
		for (int i = 0; i < txCount; i++) {
			TransactionRequest txRequest = buildTransactionRequest_RandomOperation(ledgerHash, cryptoSetting);
			TransactionResult txResult = buildTransactionResult(txRequest, blockHeight,
					TX_EXEC_STATES[i % TX_EXEC_STATES.length]);

			txRequests[i] = txRequest;
			txResults[i] = txResult;
		}
	}

	private void assertTransactionEquals(TransactionResult txExpected, TransactionResult txActual) {
		assertEquals(txExpected.getTransactionHash(), txActual.getTransactionHash());
		assertEquals(txExpected.getExecutionState(), txActual.getExecutionState());
		assertEquals(txExpected.getBlockHeight(), txActual.getBlockHeight());

		assertEquals(txExpected.getOperationResults().length, txActual.getOperationResults().length);

		assertEquals(txExpected.getDataSnapshot().getAdminAccountHash(),
				txActual.getDataSnapshot().getAdminAccountHash());
		assertEquals(txExpected.getDataSnapshot().getContractAccountSetHash(),
				txActual.getDataSnapshot().getContractAccountSetHash());
		assertEquals(txExpected.getDataSnapshot().getDataAccountSetHash(),
				txActual.getDataSnapshot().getDataAccountSetHash());
		assertEquals(txExpected.getDataSnapshot().getUserAccountSetHash(),
				txActual.getDataSnapshot().getUserAccountSetHash());

	}

	private TransactionResult buildTransactionResult(TransactionRequest txReq, long blockHeight,
			TransactionState txState) {
		TransactionStagedSnapshot txSnapshot = new TransactionStagedSnapshot();
		HashDigest adminAccountHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setAdminAccountHash(adminAccountHash);
		HashDigest userAccountSetHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setUserAccountSetHash(userAccountSetHash);
		HashDigest dataAccountSetHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setDataAccountSetHash(dataAccountSetHash);
		HashDigest contractAccountSetHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setContractAccountSetHash(contractAccountSetHash);

		OperationResult[] opResults = new OperationResult[0];
		TransactionResultData txResult = new TransactionResultData(txReq.getTransactionHash(), blockHeight, txState,
				txSnapshot, opResults);

		return txResult;
	}

	/**
	 * 创建交易请求；以随机生成的数据作为交易的操作参数；
	 * 
	 * @param ledgerHash
	 * @param defCryptoSetting
	 * @return
	 */
	private TransactionRequest buildTransactionRequest_RandomOperation(HashDigest ledgerHash,
			CryptoSetting defCryptoSetting) {
		// Build transaction request;
		TxBuilder txBuilder = new TxBuilder(ledgerHash, defCryptoSetting.getHashAlgorithm());

		Operation[] operations = new Operation[5];

		// register user;
		BlockchainKeypair userKey = BlockchainKeyGenerator.getInstance().generate();
		operations[0] = txBuilder.users().register(userKey.getIdentity());

		// register data account;
		BlockchainKeypair dataKey = BlockchainKeyGenerator.getInstance().generate();
		operations[1] = txBuilder.dataAccounts().register(dataKey.getIdentity());

		// set data after registering data account immediately;
		operations[2] = txBuilder.dataAccount(dataKey.getAddress()).setText("A", "Value_A_0", -1)
				.setText("B", "Value_B_0", -1).getOperation();

		// generate random bytes as the bytes of ChainCode to deploy as a smart
		// contract；
		byte[] chainCode = new byte[128];
		rand.nextBytes(chainCode);
		BlockchainKeypair contractKey = BlockchainKeyGenerator.getInstance().generate();
		operations[3] = txBuilder.contracts().deploy(contractKey.getIdentity(), chainCode);

		// invoke smart contract;
		operations[4] = txBuilder.contract(contractKey.getAddress()).invoke("test",
				BytesDataList.singleText("TestContractArgs"));

		// build transaction request;
		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest();

		BlockchainKeypair sponsorKey = BlockchainKeyGenerator.getInstance().generate();
		txReqBuilder.signAsEndpoint(sponsorKey);
		BlockchainKeypair gatewayKey = BlockchainKeyGenerator.getInstance().generate();
		txReqBuilder.signAsNode(gatewayKey);

		TransactionRequest txReq = txReqBuilder.buildRequest();

		return txReq;
	}

	/**
	 * 根据实际运行中一个随机出现的错误中提取到的数据来建立的测试用例，可以更简化地验证正确性；
	 * 
	 * <p>
	 * 
	 * 注：重构了 {@link LedgerTransaction} 和 {@link TransactionContent}
	 * 等交易结构相关的类型之后，此用例已经失效； by huanghaiquan on 2020-09-16;
	 */
	// @Test
	public void testSpecialCase_1() {
		CryptoSetting defCryptoSetting = LedgerTestUtils.createDefaultCryptoSetting();

		MemoryKVStorage testStorage = new MemoryKVStorage();

		BufferedKVStorage bufferStorage = new BufferedKVStorage(null, testStorage, testStorage, false);

		// Create a new TransactionSet, it's empty;
		TransactionSetEditor txset = new TransactionSetEditor(defCryptoSetting, keyPrefix, bufferStorage, bufferStorage, LedgerDataStructure.MERKLE_TREE);
		assertTrue(txset.isUpdated());
		assertFalse(txset.isReadonly());
		assertNull(txset.getRootHash());

		HashDigest ledgerHash = Crypto.resolveAsHashDigest(Base58Utils.decode("j5iF5xJ7KN4kjRrhD3EUKVSPmHz2bExxp3h9avqxcnnzch"));
		assertEquals("j5iF5xJ7KN4kjRrhD3EUKVSPmHz2bExxp3h9avqxcnnzch", ledgerHash.toBase58());

		BlockchainKeypair parti0 = LedgerTestUtils.createKeyPair("7VeRLBwqTAz8oRazEazeaEfqei46sk2FzvBgyHMUBJvrUEGT",
				"7VeRUm27GbrsX9HbQSZguChLp24HZYub6s5FJ7FjBht8BmbA");

		BlockchainKeypair userKeypair1 = LedgerTestUtils.createKeyPair(
				"7VeRKf3GFLFcBfzvtzmtyMXEoX2HYGEJ4j7CmHcnRV99W5Dp", "7VeRYQjeAaQY5Po8MMtmGNHA2SniqLXmJaZwBS5K8zTtMAU1");
		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_UserReg_SHA256(userKeypair1,
				ledgerHash, 1580315317127L, parti0, parti0);
//		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_UserReg(userKeypair1, ledgerHash, 202001202020L,
//				parti0, parti0);
		System.out.printf("\r\n ===||=== transactionRequest1.getTransactionHash()=[%s]\r\n",
				transactionRequest1.getTransactionHash().toBase58());
//		assertEquals("j5sXmpcomtM2QMUNWeQWsF8bNFFnyeXoCjVAekEeLSscgY", transactionRequest1.getTransactionHash().toBase58());
		assertEquals("j5wPGKT5CUzwi8j6VfCWaP2p9YZ6WVWtMANp9HbHWzvhgG",
				transactionRequest1.getTransactionHash().toBase58());

		TransactionStagedSnapshot txSnapshot = new TransactionStagedSnapshot();
		txSnapshot.setAdminAccountHash(
				Crypto.resolveAsHashDigest(Base58Utils.decode("j5taeK6cpmJGcn8QbEYCqadna6s7NDSheDTK6NJdU4mFhh")));
		txSnapshot.setUserAccountSetHash(
				Crypto.resolveAsHashDigest(Base58Utils.decode("j5oQDSob92mCoGSHtrXa9soqgAtMyjwfRMt2kj7igXXJrP")));

		TransactionResult tx = new TransactionResultData(transactionRequest1.getTransactionHash(), 1,
				TransactionState.SUCCESS, txSnapshot);
		txset.addTransaction(transactionRequest1, tx);

		LedgerTransaction tx_query = txset.getTransaction(transactionRequest1.getTransactionHash());
		assertNotNull(tx_query);

		txset.commit();
		bufferStorage.commit();

		tx_query = txset.getTransaction(transactionRequest1.getTransactionHash());
		TransactionState tx_state = txset.getState(transactionRequest1.getTransactionHash());
		assertNotNull(tx_query);
		assertEquals(0, tx_state.CODE);

		HashDigest txsetRootHash = txset.getRootHash();

		txset = new TransactionSetEditor(-1, txsetRootHash, defCryptoSetting, keyPrefix, testStorage, testStorage, LedgerDataStructure.MERKLE_TREE, false);
		tx_query = txset.getTransaction(transactionRequest1.getTransactionHash());
		tx_state = txset.getState(transactionRequest1.getTransactionHash());

		assertNotNull(tx_query);
		assertEquals(0, tx_state.CODE);
	}

}
