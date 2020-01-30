package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainKeyGenerator;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.DataVersionConflictException;
import com.jd.blockchain.ledger.EndpointRequest;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.NodeRequest;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionContentBody;
import com.jd.blockchain.ledger.TransactionPermission;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.DataAccount;
import com.jd.blockchain.ledger.core.DefaultOperationHandleRegisteration;
import com.jd.blockchain.ledger.core.LedgerDataQuery;
import com.jd.blockchain.ledger.core.LedgerEditor;
import com.jd.blockchain.ledger.core.LedgerManager;
import com.jd.blockchain.ledger.core.LedgerRepository;
import com.jd.blockchain.ledger.core.LedgerSecurityManager;
import com.jd.blockchain.ledger.core.OperationHandleRegisteration;
import com.jd.blockchain.ledger.core.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionBatchProcessor;
import com.jd.blockchain.ledger.core.TransactionSet;
import com.jd.blockchain.ledger.core.UserAccount;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;

public class TransactionBatchProcessorTest {
	static {
		DataContractRegistry.register(TransactionContent.class);
		DataContractRegistry.register(TransactionContentBody.class);
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(NodeRequest.class);
		DataContractRegistry.register(EndpointRequest.class);
		DataContractRegistry.register(TransactionResponse.class);
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(DataAccountRegisterOperation.class);
	}

//	private HashDigest ledgerHash = null;

	private static BlockchainKeypair parti0;
	private static BlockchainKeypair parti1;
	private static BlockchainKeypair parti2;
	private static BlockchainKeypair parti3;

	private static BlockchainKeypair[] participants;

	// TODO: 验证无效签名会被拒绝；

	static {
//		parti0 = BlockchainKeyGenerator.getInstance().generate();
//		parti1 = BlockchainKeyGenerator.getInstance().generate();
//		parti2 = BlockchainKeyGenerator.getInstance().generate();
//		parti3 = BlockchainKeyGenerator.getInstance().generate();

		// TX: j5iy6xJNYGxmr2Puh6G4nzrYpVYXuHf7YQMsFhxq8Wh1K6
//		Parti0 -- PUB:[7VeRLBwqTAz8oRazEazeaEfqei46sk2FzvBgyHMUBJvrUEGT]; PRIV:[7VeRUm27GbrsX9HbQSZguChLp24HZYub6s5FJ7FjBht8BmbA]
//		Parti1 -- PUB:[7VeRNJasZp76ThmUkoAajJEduotS4JC6T9wzhz9TDPvjLCRk]; PRIV:[7VeRcBcPkTZ4hFwfcKRgFJWdDesHyysQWkKYC6xfPApbfvwQ]
//		Parti2 -- PUB:[7VeR7uSd7sqxkMp73936MoK7eUSmGPVrsmrwdekiR9fmvdYN]; PRIV:[7VeRUkgMXRegHHWhezv4LdJV6oQuSXo6Ezp2sjC2M5NTUWkz]
//		Parti3 -- PUB:[7VeR8X8fa9th42XSXvnuBLfR4v3dxjXq6jPfvF7nDPB2MTo1]; PRIV:[7VeRdreAev1E8ySsLWX7rRMArh5wHBTmZXKwNUuoVo7cBn6o]
		parti0 = LedgerTestUtils.createKeyPair("7VeRLBwqTAz8oRazEazeaEfqei46sk2FzvBgyHMUBJvrUEGT",
				"7VeRUm27GbrsX9HbQSZguChLp24HZYub6s5FJ7FjBht8BmbA");
		parti1 = LedgerTestUtils.createKeyPair("7VeRNJasZp76ThmUkoAajJEduotS4JC6T9wzhz9TDPvjLCRk",
				"7VeRcBcPkTZ4hFwfcKRgFJWdDesHyysQWkKYC6xfPApbfvwQ");
		parti2 = LedgerTestUtils.createKeyPair("7VeR7uSd7sqxkMp73936MoK7eUSmGPVrsmrwdekiR9fmvdYN",
				"7VeRUkgMXRegHHWhezv4LdJV6oQuSXo6Ezp2sjC2M5NTUWkz");
		parti3 = LedgerTestUtils.createKeyPair("7VeR8X8fa9th42XSXvnuBLfR4v3dxjXq6jPfvF7nDPB2MTo1",
				"7VeRdreAev1E8ySsLWX7rRMArh5wHBTmZXKwNUuoVo7cBn6o");

		participants = new BlockchainKeypair[] { parti0, parti1, parti2, parti3 };
	}

	@Test
	public void testSingleTxProcess() {
		final MemoryKVStorage STORAGE = new MemoryKVStorage();

		// 初始化账本到指定的存储库；
		HashDigest ledgerHash = LedgerTestUtils.initLedger(STORAGE, parti0, parti1, parti2, parti3);

		// 加载账本；
		LedgerManager ledgerManager = new LedgerManager();
		LedgerRepository ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);

		// 验证参与方账户的存在；
		LedgerDataQuery previousBlockDataset = ledgerRepo.getLedgerData(ledgerRepo.getLatestBlock());
		UserAccount user0 = previousBlockDataset.getUserAccountSet().getAccount(parti0.getAddress());
		assertNotNull(user0);
		boolean partiRegistered = previousBlockDataset.getUserAccountSet().contains(parti0.getAddress());
		assertTrue(partiRegistered);

		// 生成新区块；
		LedgerEditor newBlockEditor = ledgerRepo.createNextBlock();

		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
		LedgerSecurityManager securityManager = getSecurityManager();
		TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(securityManager, newBlockEditor,
				ledgerRepo, opReg);

		// 注册新用户；
		BlockchainKeypair userKeypair = BlockchainKeyGenerator.getInstance().generate();
		TransactionRequest transactionRequest = LedgerTestUtils.createTxRequest_UserReg(userKeypair, ledgerHash, parti0,
				parti0);
		TransactionResponse txResp = txbatchProcessor.schedule(transactionRequest);

		LedgerBlock newBlock = newBlockEditor.prepare();
		newBlockEditor.commit();

		// 验证正确性；
		ledgerManager = new LedgerManager();
		ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);

		LedgerBlock latestBlock = ledgerRepo.getLatestBlock();
		assertEquals(newBlock.getHash(), latestBlock.getHash());
		assertEquals(1, newBlock.getHeight());

		assertEquals(TransactionState.SUCCESS, txResp.getExecutionState());
	}

	private static LedgerSecurityManager getSecurityManager() {
		LedgerSecurityManager securityManager = Mockito.mock(LedgerSecurityManager.class);

		SecurityPolicy securityPolicy = Mockito.mock(SecurityPolicy.class);
		when(securityPolicy.isEndpointEnable(any(LedgerPermission.class), any())).thenReturn(true);
		when(securityPolicy.isEndpointEnable(any(TransactionPermission.class), any())).thenReturn(true);
		when(securityPolicy.isNodeEnable(any(LedgerPermission.class), any())).thenReturn(true);
		when(securityPolicy.isNodeEnable(any(TransactionPermission.class), any())).thenReturn(true);

		when(securityManager.createSecurityPolicy(any(), any())).thenReturn(securityPolicy);

		return securityManager;
	}

	@Test
	public void testMultiTxsProcess() {
		final MemoryKVStorage STORAGE = new MemoryKVStorage();

		// 初始化账本到指定的存储库；
		HashDigest ledgerHash = LedgerTestUtils.initLedger(STORAGE, parti0, parti1, parti2, parti3);

		// 加载账本；
		LedgerManager ledgerManager = new LedgerManager();
		LedgerRepository ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);

		// 验证参与方账户的存在；
		LedgerDataQuery previousBlockDataset = ledgerRepo.getLedgerData(ledgerRepo.getLatestBlock());
		UserAccount user0 = previousBlockDataset.getUserAccountSet().getAccount(parti0.getAddress());
		assertNotNull(user0);
		boolean partiRegistered = previousBlockDataset.getUserAccountSet().contains(parti0.getAddress());
		assertTrue(partiRegistered);

		// 生成新区块；
		LedgerEditor newBlockEditor = ledgerRepo.createNextBlock();

		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
		LedgerSecurityManager securityManager = getSecurityManager();
		TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(securityManager, newBlockEditor,
				ledgerRepo, opReg);

		// 注册新用户；
		BlockchainKeypair userKeypair1 = BlockchainKeyGenerator.getInstance().generate();
		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_UserReg(userKeypair1, ledgerHash,
				parti0, parti0);
		TransactionResponse txResp1 = txbatchProcessor.schedule(transactionRequest1);

		BlockchainKeypair userKeypair2 = BlockchainKeyGenerator.getInstance().generate();
		TransactionRequest transactionRequest2 = LedgerTestUtils.createTxRequest_UserReg(userKeypair2, ledgerHash,
				parti0, parti0);
		TransactionResponse txResp2 = txbatchProcessor.schedule(transactionRequest2);

		LedgerBlock newBlock = newBlockEditor.prepare();
		newBlockEditor.commit();

		assertEquals(TransactionState.SUCCESS, txResp1.getExecutionState());
		assertEquals(TransactionState.SUCCESS, txResp2.getExecutionState());

		// 验证正确性；
		ledgerManager = new LedgerManager();
		ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);

		LedgerBlock latestBlock = ledgerRepo.getLatestBlock();
		assertEquals(newBlock.getHash(), latestBlock.getHash());
		assertEquals(1, newBlock.getHeight());

		LedgerDataQuery ledgerDS = ledgerRepo.getLedgerData(latestBlock);
		boolean existUser1 = ledgerDS.getUserAccountSet().contains(userKeypair1.getAddress());
		boolean existUser2 = ledgerDS.getUserAccountSet().contains(userKeypair2.getAddress());
		assertTrue(existUser1);
		assertTrue(existUser2);
	}

	@Test
	public void testTxRollback() {
		System.out.println("------------ keys -----------");
		System.out.printf("Parti0 -- PUB:[%s]; PRIV:[%s]\r\n", parti0.getPubKey().toBase58(),
				parti0.getPrivKey().toBase58());
		System.out.printf("Parti1 -- PUB:[%s]; PRIV:[%s]\r\n", parti1.getPubKey().toBase58(),
				parti1.getPrivKey().toBase58());
		System.out.printf("Parti2 -- PUB:[%s]; PRIV:[%s]\r\n", parti2.getPubKey().toBase58(),
				parti2.getPrivKey().toBase58());
		System.out.printf("Parti3 -- PUB:[%s]; PRIV:[%s]\r\n", parti3.getPubKey().toBase58(),
				parti3.getPrivKey().toBase58());
		System.out.println("------------ end-keys -----------");

		final MemoryKVStorage STORAGE = new MemoryKVStorage();

		// 初始化账本到指定的存储库；
		HashDigest ledgerHash = LedgerTestUtils.initLedger(STORAGE, parti0, parti1, parti2, parti3);

		System.out.printf("\r\n------------ LEDGER [%s] -----------\r\n", ledgerHash.toBase58());

		// 加载账本；
		LedgerManager ledgerManager = new LedgerManager();
		LedgerRepository ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);
		CryptoSetting cryptoSetting = ledgerRepo.getAdminSettings().getSettings().getCryptoSetting();

		// 验证参与方账户的存在；
		LedgerDataQuery previousBlockDataset = ledgerRepo.getLedgerData(ledgerRepo.getLatestBlock());
		UserAccount user0 = previousBlockDataset.getUserAccountSet().getAccount(parti0.getAddress());
		assertNotNull(user0);
		boolean partiRegistered = previousBlockDataset.getUserAccountSet().contains(parti0.getAddress());
		assertTrue(partiRegistered);

		// 生成新区块；
		LedgerEditor newBlockEditor = ledgerRepo.createNextBlock();

		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
		LedgerSecurityManager securityManager = getSecurityManager();
		TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(securityManager, newBlockEditor,
				ledgerRepo, opReg);

		// 注册新用户；
//		BlockchainKeypair userKeypair1 = BlockchainKeyGenerator.getInstance().generate();
		BlockchainKeypair userKeypair1 = LedgerTestUtils.createKeyPair(
				"7VeRKf3GFLFcBfzvtzmtyMXEoX2HYGEJ4j7CmHcnRV99W5Dp", "7VeRYQjeAaQY5Po8MMtmGNHA2SniqLXmJaZwBS5K8zTtMAU1");
		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_UserReg(userKeypair1, ledgerHash,
				1580315317127L,parti0, parti0);
		//错误参数：ts=1580315317127; txhash=j5wPGKT5CUzwi8j6VfCWaP2p9YZ6WVWtMANp9HbHWzvhgG
		System.out.printf("\r\n ===||=== transactionRequest1.getTransactionContent().getHash()=[%s]\r\n",
				 transactionRequest1.getTransactionContent().getHash().toBase58());
		TransactionResponse txResp1 = txbatchProcessor.schedule(transactionRequest1);

//		BlockchainKeypair userKeypair2 = BlockchainKeyGenerator.getInstance().generate();
		BlockchainKeypair userKeypair2 = LedgerTestUtils.createKeyPair(
				"7VeRKSnDFveTfLLMsLZDmmhGmgf7i142XHgBFjnrKuS95tY3", "7VeRTiJ2TpQD9aBi29ajnqdntgoVBANmC3oCbHThKb5tzfTJ");
		TransactionRequest transactionRequest2 = LedgerTestUtils.createTxRequest_MultiOPs_WithNotExistedDataAccount(
				userKeypair2, ledgerHash, 202001202020L, parti0, parti0);
		System.out.printf("\r\n ===||=== transactionRequest2.getTransactionContent().getHash()=[%s]\r\n",
				transactionRequest2.getTransactionContent().getHash().toBase58());
		TransactionResponse txResp2 = txbatchProcessor.schedule(transactionRequest2);

//		BlockchainKeypair userKeypair3 = BlockchainKeyGenerator.getInstance().generate();
		BlockchainKeypair userKeypair3 = LedgerTestUtils.createKeyPair(
				"7VeRDoaSexqLWKkaZyrQwdwSuE9n5nszduMrYBfYRfEkREQV", "7VeRdFtTuLfrzCYJzQ6enQUkGTc83ATgjr8WbmfjBQuTFpHt");
		TransactionRequest transactionRequest3 = LedgerTestUtils.createTxRequest_UserReg(userKeypair3, ledgerHash,
				202001202020L, parti0, parti0);
		System.out.printf("\r\n ===||=== transactionRequest3.getTransactionContent().getHash()=[%s]\r\n",
				transactionRequest3.getTransactionContent().getHash().toBase58());
		TransactionResponse txResp3 = txbatchProcessor.schedule(transactionRequest3);

		LedgerBlock newBlock = newBlockEditor.prepare();
		newBlockEditor.commit();

		// 在重新加载之前验证一次； 
		long blockHeight = newBlock.getHeight();
		assertEquals(1, blockHeight);
		HashDigest blockHash = newBlock.getHash();
		assertNotNull(blockHash);
		assertEquals(TransactionState.SUCCESS, txResp1.getExecutionState());
		assertEquals(TransactionState.DATA_ACCOUNT_DOES_NOT_EXIST, txResp2.getExecutionState());
		assertEquals(TransactionState.SUCCESS, txResp3.getExecutionState());

		LedgerTransaction tx1 = ledgerRepo.getTransactionSet()
				.get(transactionRequest1.getTransactionContent().getHash());
		LedgerTransaction tx2 = ledgerRepo.getTransactionSet()
				.get(transactionRequest2.getTransactionContent().getHash());
		LedgerTransaction tx3 = ledgerRepo.getTransactionSet()
				.get(transactionRequest3.getTransactionContent().getHash());

		assertNotNull(tx3);
		assertEquals(TransactionState.SUCCESS, tx3.getExecutionState());
		assertNotNull(tx2);
		assertEquals(TransactionState.DATA_ACCOUNT_DOES_NOT_EXIST, tx2.getExecutionState());
		assertNotNull(tx1);
		assertEquals(TransactionState.SUCCESS, tx1.getExecutionState());

		HashDigest txsetRootHash = ledgerRepo.getTransactionSet().getRootHash();

		// 单独加载交易集合；
		TransactionSet txset = new TransactionSet(txsetRootHash, cryptoSetting, "LDG://3A3dP4", STORAGE, STORAGE,
				false);
		tx1 = txset.get(transactionRequest1.getTransactionContent().getHash());
//		tx2 = txset.get(transactionRequest2.getTransactionContent().getHash());
		tx3 = txset.get(transactionRequest3.getTransactionContent().getHash());
		
		assertNotNull(tx3);
//		assertNotNull(tx2);
		assertNotNull(tx1);

		// 重新加载之后验证正确性；
		ledgerManager = new LedgerManager();
		ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);

		LedgerBlock latestBlock = ledgerRepo.getLatestBlock();
		assertEquals(blockHash, latestBlock.getHash());
		assertEquals(blockHeight, latestBlock.getHeight());

		assertEquals(txsetRootHash, ledgerRepo.getTransactionSet().getRootHash());

		tx1 = ledgerRepo.getTransactionSet().get(transactionRequest1.getTransactionContent().getHash());
		tx2 = ledgerRepo.getTransactionSet().get(transactionRequest2.getTransactionContent().getHash());
		tx3 = ledgerRepo.getTransactionSet().get(transactionRequest3.getTransactionContent().getHash());

		assertNotNull(tx1);
		assertEquals(TransactionState.SUCCESS, tx1.getExecutionState());
		assertNotNull(tx2);
		assertEquals(TransactionState.DATA_ACCOUNT_DOES_NOT_EXIST, tx2.getExecutionState());
		assertNotNull(tx3);
		assertEquals(TransactionState.SUCCESS, tx3.getExecutionState());

		LedgerDataQuery ledgerDS = ledgerRepo.getLedgerData(latestBlock);
		boolean existUser1 = ledgerDS.getUserAccountSet().contains(userKeypair1.getAddress());
		boolean existUser2 = ledgerDS.getUserAccountSet().contains(userKeypair2.getAddress());
		boolean existUser3 = ledgerDS.getUserAccountSet().contains(userKeypair3.getAddress());
		assertTrue(existUser1);
		assertFalse(existUser2);
		assertTrue(existUser3);
	}

	@Test
	public void testTxRollbackByVersionsConflict() {
		final MemoryKVStorage STORAGE = new MemoryKVStorage();

		// 初始化账本到指定的存储库；
		HashDigest ledgerHash = LedgerTestUtils.initLedger(STORAGE, parti0, parti1, parti2, parti3);

		// 加载账本；
		LedgerManager ledgerManager = new LedgerManager();
		LedgerRepository ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);

		// 验证参与方账户的存在；
		LedgerDataQuery previousBlockDataset = ledgerRepo.getLedgerData(ledgerRepo.getLatestBlock());
		UserAccount user0 = previousBlockDataset.getUserAccountSet().getAccount(parti0.getAddress());
		assertNotNull(user0);
		boolean partiRegistered = previousBlockDataset.getUserAccountSet().contains(parti0.getAddress());
		assertTrue(partiRegistered);

		// 注册数据账户；
		// 生成新区块；
		LedgerEditor newBlockEditor = ledgerRepo.createNextBlock();

		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
		LedgerSecurityManager securityManager = getSecurityManager();
		TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(securityManager, newBlockEditor,
				ledgerRepo, opReg);

		BlockchainKeypair dataAccountKeypair = BlockchainKeyGenerator.getInstance().generate();
		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_DataAccountReg(dataAccountKeypair,
				ledgerHash, parti0, parti0);
		TransactionResponse txResp1 = txbatchProcessor.schedule(transactionRequest1);
		LedgerBlock newBlock = newBlockEditor.prepare();
		newBlockEditor.commit();

		assertEquals(TransactionState.SUCCESS, txResp1.getExecutionState());
		DataAccount dataAccount = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress());
		assertNotNull(dataAccount);

		// 正确写入 KV 数据；
		TransactionRequest txreq1 = LedgerTestUtils.createTxRequest_DataAccountWrite(dataAccountKeypair.getAddress(),
				"K1", "V-1-1", -1, ledgerHash, parti0, parti0);
		TransactionRequest txreq2 = LedgerTestUtils.createTxRequest_DataAccountWrite(dataAccountKeypair.getAddress(),
				"K2", "V-2-1", -1, ledgerHash, parti0, parti0);
		TransactionRequest txreq3 = LedgerTestUtils.createTxRequest_DataAccountWrite(dataAccountKeypair.getAddress(),
				"K3", "V-3-1", -1, ledgerHash, parti0, parti0);

		// 连续写 K1，K1的版本将变为1；
		TransactionRequest txreq4 = LedgerTestUtils.createTxRequest_DataAccountWrite(dataAccountKeypair.getAddress(),
				"K1", "V-1-2", 0, ledgerHash, parti0, parti0);

		newBlockEditor = ledgerRepo.createNextBlock();
		previousBlockDataset = ledgerRepo.getLedgerData(ledgerRepo.getLatestBlock());
		txbatchProcessor = new TransactionBatchProcessor(securityManager, newBlockEditor, ledgerRepo, opReg);

		txbatchProcessor.schedule(txreq1);
		txbatchProcessor.schedule(txreq2);
		txbatchProcessor.schedule(txreq3);
		txbatchProcessor.schedule(txreq4);

		newBlock = newBlockEditor.prepare();
		newBlockEditor.commit();

		BytesValue v1_0 = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getValue("K1", 0);
		BytesValue v1_1 = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getValue("K1", 1);
		BytesValue v2 = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getValue("K2", 0);
		BytesValue v3 = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getValue("K3", 0);

		assertNotNull(v1_0);
		assertNotNull(v1_1);
		assertNotNull(v2);
		assertNotNull(v3);

		assertEquals("V-1-1", v1_0.getBytes().toUTF8String());
		assertEquals("V-1-2", v1_1.getBytes().toUTF8String());
		assertEquals("V-2-1", v2.getBytes().toUTF8String());
		assertEquals("V-3-1", v3.getBytes().toUTF8String());

		// 提交多笔数据写入的交易，包含存在数据版本冲突的交易，验证交易是否正确回滚；
		// 先写一笔正确的交易； k3 的版本将变为 1 ；
		TransactionRequest txreq5 = LedgerTestUtils.createTxRequest_DataAccountWrite(dataAccountKeypair.getAddress(),
				"K3", "V-3-2", 0, ledgerHash, parti0, parti0);
		// 指定冲突的版本号，正确的应该是版本1；
		TransactionRequest txreq6 = LedgerTestUtils.createTxRequest_DataAccountWrite(dataAccountKeypair.getAddress(),
				"K1", "V-1-3", 0, ledgerHash, parti0, parti0);

		newBlockEditor = ledgerRepo.createNextBlock();
		previousBlockDataset = ledgerRepo.getLedgerData(ledgerRepo.getLatestBlock());
		txbatchProcessor = new TransactionBatchProcessor(securityManager, newBlockEditor, ledgerRepo, opReg);

		txbatchProcessor.schedule(txreq5);
		// 预期会产生版本冲突异常； DataVersionConflictionException;
		DataVersionConflictException versionConflictionException = null;
		try {
			txbatchProcessor.schedule(txreq6);
		} catch (DataVersionConflictException e) {
			versionConflictionException = e;
		}
//		assertNotNull(versionConflictionException);

		newBlock = newBlockEditor.prepare();
		newBlockEditor.commit();

		BytesValue v1 = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getValue("K1");
		v3 = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset().getValue("K3");

		// k1 的版本仍然为1，没有更新；
		long k1_version = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getVersion("K1");
		assertEquals(1, k1_version);

		long k3_version = ledgerRepo.getDataAccountSet().getAccount(dataAccountKeypair.getAddress()).getDataset()
				.getVersion("K3");
		assertEquals(1, k3_version);

		assertNotNull(v1);
		assertNotNull(v3);
		assertEquals("V-1-2", v1.getBytes().toUTF8String());
		assertEquals("V-3-2", v3.getBytes().toUTF8String());

//		// 验证正确性；
//		ledgerManager = new LedgerManager();
//		ledgerRepo = ledgerManager.register(ledgerHash, STORAGE);
//
//		LedgerBlock latestBlock = ledgerRepo.getLatestBlock();
//		assertEquals(newBlock.getHash(), latestBlock.getHash());
//		assertEquals(1, newBlock.getHeight());
//
//		LedgerTransaction tx1 = ledgerRepo.getTransactionSet()
//				.get(transactionRequest1.getTransactionContent().getHash());
//
//		assertNotNull(tx1);
//		assertEquals(TransactionState.SUCCESS, tx1.getExecutionState());

	}

}
