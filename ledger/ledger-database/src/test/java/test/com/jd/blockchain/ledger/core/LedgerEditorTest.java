package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.crypto.service.classic.ClassicCryptoService;
import com.jd.blockchain.crypto.service.sm.SMCryptoService;
import com.jd.blockchain.ledger.BlockchainKeyGenerator;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DataType;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerInitSetting;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.core.DataAccount;
import com.jd.blockchain.ledger.core.LedgerDataset;
import com.jd.blockchain.ledger.core.LedgerEditor;
import com.jd.blockchain.ledger.core.LedgerManager;
import com.jd.blockchain.ledger.core.LedgerRepository;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.LedgerTransactionalEditor;
import com.jd.blockchain.ledger.core.MerkleHashDataset;
import com.jd.blockchain.ledger.core.TransactionQuery;
import com.jd.blockchain.ledger.core.UserAccount;
import com.jd.blockchain.ledger.proof.MerkleHashTrie;
import com.jd.blockchain.ledger.proof.MerkleData;
import com.jd.blockchain.storage.service.KVStorageService;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.codec.Base58Utils;

public class LedgerEditorTest {

	private static final String[] SUPPORTED_PROVIDERS = { ClassicCryptoService.class.getName(),
			SMCryptoService.class.getName() };

	static {
		DataContractRegistry.register(com.jd.blockchain.ledger.TransactionContent.class);
		DataContractRegistry.register(com.jd.blockchain.ledger.UserRegisterOperation.class);
		DataContractRegistry.register(com.jd.blockchain.ledger.BlockBody.class);
	}

	private static final String LEDGER_KEY_PREFIX = "LDG://";
	private SignatureFunction signatureFunction;

	private BlockchainKeypair parti0 = BlockchainKeyGenerator.getInstance().generate();
	private BlockchainKeypair parti1 = BlockchainKeyGenerator.getInstance().generate();
	private BlockchainKeypair parti2 = BlockchainKeyGenerator.getInstance().generate();
	private BlockchainKeypair parti3 = BlockchainKeyGenerator.getInstance().generate();

	private BlockchainKeypair[] participants = { parti0, parti1, parti2, parti3 };

	/**
	 * 初始化一个;
	 */
	@Before
	public void beforeTest() {
		signatureFunction = Crypto.getSignatureFunction("ED25519");
	}

	/**
	 * @return
	 */
	private LedgerEditor createLedgerInitEditor() {
		// 存储；
		MemoryKVStorage storage = new MemoryKVStorage();

		return createLedgerInitEditor(storage);
	}

	/**
	 * @return
	 */
	private LedgerEditor createLedgerInitEditor(KVStorageService storage) {
		CryptoSetting cryptoSetting = LedgerTestUtils.createDefaultCryptoSetting();
		return createLedgerInitEditor(cryptoSetting, storage);
	}

	/**
	 * @return
	 */
	private LedgerEditor createLedgerInitEditor(CryptoSetting cryptoSetting, KVStorageService storage) {
		// 创建初始化配置；
		LedgerInitSetting initSetting = LedgerTestUtils.createLedgerInitSetting(cryptoSetting);

		// 创建账本；
		return LedgerTransactionalEditor.createEditor(initSetting, LEDGER_KEY_PREFIX, storage.getExPolicyKVStorage(),
				storage.getVersioningKVStorage());
	}

	private LedgerTransactionContext createGenisisTx(LedgerEditor ldgEdt, BlockchainKeypair[] partis) {
		TransactionRequest genesisTxReq = LedgerTestUtils.createLedgerInitTxRequest(partis);

		LedgerTransactionContext txCtx = ldgEdt.newTransaction(genesisTxReq);

		return txCtx;
	}

	@SuppressWarnings("unused")
	@Test
	public void testWriteDataAccoutKvOp() {
		MemoryKVStorage storage = new MemoryKVStorage();

		LedgerEditor ldgEdt = createLedgerInitEditor(storage);

		LedgerTransactionContext genisisTxCtx = createGenisisTx(ldgEdt, participants);
		LedgerDataset ldgDS = genisisTxCtx.getDataset();

		AsymmetricKeypair cryptoKeyPair = signatureFunction.generateKeypair();
		BlockchainKeypair dataKP = new BlockchainKeypair(cryptoKeyPair.getPubKey(), cryptoKeyPair.getPrivKey());

		DataAccount dataAccount = ldgDS.getDataAccountSet().register(dataKP.getAddress(), dataKP.getPubKey(), null);

		dataAccount.getDataset().setValue("A", TypedValue.fromText("abc"), -1);

		LedgerTransaction tx = genisisTxCtx.commit(TransactionState.SUCCESS);
		LedgerBlock block = ldgEdt.prepare();
		// 提交数据，写入存储；
		ldgEdt.commit();

		// 预期这是第1个区块；
		assertNotNull(block);
		assertNotNull(block.getHash());
		assertEquals(0, block.getHeight());

		// 验证数据读写的一致性；
		BytesValue bytes = dataAccount.getDataset().getValue("A");
		assertEquals(DataType.TEXT, bytes.getType());
		String textValue = bytes.getBytes().toUTF8String();
		assertEquals("abc", textValue);

		// 验证重新加载的正确性；
		LedgerManager manager = new LedgerManager();
		HashDigest ledgerHash = block.getHash();
		LedgerRepository repo = manager.register(ledgerHash, storage);

		dataAccount = repo.getDataAccountSet().getAccount(dataKP.getAddress());
		assertNotNull(dataAccount);
		bytes = dataAccount.getDataset().getValue("A");
		assertEquals(DataType.TEXT, bytes.getType());
		textValue = bytes.getBytes().toUTF8String();
		assertEquals("abc", textValue);

		LedgerTransaction tx_init = repo.getTransactionSet().get(tx.getTransactionContent().getHash());
		assertNotNull(tx_init);
	}

	/**
	 * 测试创建账本；
	 */
	@Test
	public void testGennesisBlockCreation() {
		LedgerEditor ldgEdt = createLedgerInitEditor();
		LedgerTransactionContext genisisTxCtx = createGenisisTx(ldgEdt, participants);
		LedgerDataset ldgDS = genisisTxCtx.getDataset();

		AsymmetricKeypair cryptoKeyPair = signatureFunction.generateKeypair();
		BlockchainKeypair userKP = new BlockchainKeypair(cryptoKeyPair.getPubKey(), cryptoKeyPair.getPrivKey());
		UserAccount userAccount = ldgDS.getUserAccountSet().register(userKP.getAddress(), userKP.getPubKey());
		userAccount.setProperty("Name", "孙悟空", -1);
		userAccount.setProperty("Age", "10000", -1);

		LedgerTransaction tx = genisisTxCtx.commit(TransactionState.SUCCESS);

		TransactionRequest genesisTxReq = genisisTxCtx.getTransactionRequest();
		assertEquals(genesisTxReq.getTransactionContent().getHash(), tx.getTransactionContent().getHash());
		assertEquals(0, tx.getBlockHeight());

		LedgerBlock block = ldgEdt.prepare();

		assertEquals(0, block.getHeight());
		assertNotNull(block.getHash());
		assertNull(block.getLedgerHash());
		assertNull(block.getPreviousHash());

		// 提交数据，写入存储；
		ldgEdt.commit();

	}

	@Test
	public void testRollback() {
		BlockchainKeypair parti0 = LedgerTestUtils.createKeyPair("7VeRLBwqTAz8oRazEazeaEfqei46sk2FzvBgyHMUBJvrUEGT",
				"7VeRUm27GbrsX9HbQSZguChLp24HZYub6s5FJ7FjBht8BmbA");
		BlockchainKeypair parti1 = LedgerTestUtils.createKeyPair("7VeRNJasZp76ThmUkoAajJEduotS4JC6T9wzhz9TDPvjLCRk",
				"7VeRcBcPkTZ4hFwfcKRgFJWdDesHyysQWkKYC6xfPApbfvwQ");
		BlockchainKeypair parti2 = LedgerTestUtils.createKeyPair("7VeR7uSd7sqxkMp73936MoK7eUSmGPVrsmrwdekiR9fmvdYN",
				"7VeRUkgMXRegHHWhezv4LdJV6oQuSXo6Ezp2sjC2M5NTUWkz");
		BlockchainKeypair parti3 = LedgerTestUtils.createKeyPair("7VeR8X8fa9th42XSXvnuBLfR4v3dxjXq6jPfvF7nDPB2MTo1",
				"7VeRdreAev1E8ySsLWX7rRMArh5wHBTmZXKwNUuoVo7cBn6o");

		BlockchainKeypair[] participants = new BlockchainKeypair[] { parti0, parti1, parti2, parti3 };

		final MemoryKVStorage STORAGE = new MemoryKVStorage();

		// 初始化账本到指定的存储库；
		HashDigest ledgerHash = LedgerTestUtils.initLedger(STORAGE, parti0, parti1, parti2, parti3);

		System.out.printf("\r\n------------ LEDGER [%s] -----------\r\n", ledgerHash.toBase58());

		// 验证重新加载的正确性；
		LedgerManager manager = new LedgerManager();
		LedgerRepository repo = manager.register(ledgerHash, STORAGE);

		LedgerBlock block = repo.getBlock(ledgerHash);
		assertNotNull(block);
		assertNotNull(block.getHash());
		assertEquals(0, block.getHeight());

		// 创建交易连续交易，验证中间的交易回滚是否影响前后的交易；
		BlockchainKeypair user1 = LedgerTestUtils.createKeyPair("7VeRKf3GFLFcBfzvtzmtyMXEoX2HYGEJ4j7CmHcnRV99W5Dp",
				"7VeRYQjeAaQY5Po8MMtmGNHA2SniqLXmJaZwBS5K8zTtMAU1");
		TransactionRequest req1 = LedgerTestUtils.createTxRequest_UserReg(user1, ledgerHash, 1580315317127L, parti0,
				parti0);
		// 引发错误的参数：ts=1580315317127;
		// txhash=j5wPGKT5CUzwi8j6VfCWaP2p9YZ6WVWtMANp9HbHWzvhgG
		System.out.printf("\r\n ===||=== transactionRequest1.getTransactionContent().getHash()=[%s]\r\n",
				req1.getTransactionContent().getHash().toBase58());

		BlockchainKeypair user2 = LedgerTestUtils.createKeyPair("7VeRKSnDFveTfLLMsLZDmmhGmgf7i142XHgBFjnrKuS95tY3",
				"7VeRTiJ2TpQD9aBi29ajnqdntgoVBANmC3oCbHThKb5tzfTJ");
		TransactionRequest req2 = LedgerTestUtils.createTxRequest_MultiOPs_WithNotExistedDataAccount(user2, ledgerHash,
				202001202020L, parti0, parti0);
		System.out.printf("\r\n ===||=== transactionRequest2.getTransactionContent().getHash()=[%s]\r\n",
				req2.getTransactionContent().getHash().toBase58());

		BlockchainKeypair user3 = LedgerTestUtils.createKeyPair("7VeRDoaSexqLWKkaZyrQwdwSuE9n5nszduMrYBfYRfEkREQV",
				"7VeRdFtTuLfrzCYJzQ6enQUkGTc83ATgjr8WbmfjBQuTFpHt");
		TransactionRequest req3 = LedgerTestUtils.createTxRequest_UserReg(user3, ledgerHash, 202001202020L, parti0,
				parti0);
		System.out.printf("\r\n ===||=== transactionRequest3.getTransactionContent().getHash()=[%s]\r\n",
				req3.getTransactionContent().getHash().toBase58());

		System.out.println("\r\n--------------- Start new Block 1 --------------\r\n");
		// 创建交易；
		LedgerEditor editor = repo.createNextBlock();

		System.out.println("\r\n--------------- Start new tx1 --------------\r\n");
		LedgerTransactionContext txctx1 = editor.newTransaction(req1);
		txctx1.getDataset().getUserAccountSet().register(user1.getAddress(), user1.getPubKey());
		LedgerTransaction tx1 = txctx1.commit(TransactionState.SUCCESS);
		HashDigest txHash1 = tx1.getTransactionContent().getHash();

		System.out.println("\r\n--------------- Start new tx2 --------------\r\n");

		LedgerTransactionContext txctx2 = editor.newTransaction(req2);
		txctx2.getDataset().getUserAccountSet().register(user2.getAddress(), user2.getPubKey());
		LedgerTransaction tx2 = txctx2.discardAndCommit(TransactionState.DATA_ACCOUNT_DOES_NOT_EXIST);
		HashDigest txHash2 = tx2.getTransactionContent().getHash();

		System.out.println("\r\n--------------- Start new tx3 --------------\r\n");
		LedgerTransactionContext txctx3 = editor.newTransaction(req3);
		txctx3.getDataset().getUserAccountSet().register(user3.getAddress(), user3.getPubKey());
		LedgerTransaction tx3 = txctx3.commit(TransactionState.SUCCESS);
		HashDigest txHash3 = tx3.getTransactionContent().getHash();

		System.out.println("\r\n--------------- Start preparing new block 1 --------------\r\n");

		LedgerBlock block1 = editor.prepare();

		System.out.println("\r\n--------------- Start commiting new block 1 --------------\r\n");
		editor.commit();

		System.out.printf("\r\n--------------- End commiting new block 1 [Storage.Count=%s]--------------\r\n\r\n",
				STORAGE.getStorageCount());

		assertEquals(1, block1.getHeight());

		// 重新加载和验证；
		manager = new LedgerManager();
		repo = manager.register(ledgerHash, STORAGE);

		LedgerTransaction act_tx1 = repo.getTransactionSet().get(txHash1);
		LedgerTransaction act_tx2 = repo.getTransactionSet().get(txHash2);
		LedgerTransaction act_tx3 = repo.getTransactionSet().get(txHash3);

		assertNotNull(act_tx3);
		assertNotNull(act_tx2);
		assertNotNull(act_tx1);
	}

	@Test
	public void testMerkleDataSet1() {
		CryptoSetting setting = LedgerTestUtils.createDefaultCryptoSetting();

		Bytes keyPrefix = Bytes.fromString(LedgerTestUtils.LEDGER_KEY_PREFIX).concat(MerkleHashDataset.MERKLE_TREE_PREFIX);

		MemoryKVStorage storage = new MemoryKVStorage();

		byte[] key = Base58Utils.decode("j5q7n8ShYqKVitaobZrERtBK7GowGGZ54RuaUeWjLsdPYY");
		HashDigest valueHash = new HashDigest(Base58Utils.decode("j5o6mMnMQqE5fJKJ93FzXPnu4vFCfpBKp7u4r8tUUaFRK8"));
		long version = 0;

		MerkleHashTrie merkleTree = new MerkleHashTrie(setting, keyPrefix, storage);
		
		merkleTree.setData(key, version, valueHash);
		
		merkleTree.commit();
		
		MerkleData data = merkleTree.getData(key);
		assertNotNull(data);
		
		merkleTree = new MerkleHashTrie(merkleTree.getRootHash(), setting, keyPrefix, storage, false);
		data = merkleTree.getData(key);
		assertNotNull(data);
		
//		MerkleDataSet1 mkds = new MerkleDataSet1(setting, keyPrefix, storage, storage);
//		HashDigest ledgerHash = new HashDigest(Base58Utils.decode("j5mxiw6RiHP7fhrySjYji1ER5aRe6d2quYHArtwUfsyoHZ"));
//
//		BlockchainKeypair user1 = LedgerTestUtils.createKeyPair("7VeRKf3GFLFcBfzvtzmtyMXEoX2HYGEJ4j7CmHcnRV99W5Dp",
//				"7VeRYQjeAaQY5Po8MMtmGNHA2SniqLXmJaZwBS5K8zTtMAU1");
//		TransactionRequest req1 = LedgerTestUtils.createTxRequest_UserReg(user1, ledgerHash, 1580315317127L, parti0,
//				parti0);
//		// 引发错误的参数：ts=1580315317127;
//		// txhash=j5wPGKT5CUzwi8j6VfCWaP2p9YZ6WVWtMANp9HbHWzvhgG
//		System.out.printf("\r\n ===||=== transactionRequest1.getTransactionContent().getHash()=[%s]\r\n",
//				req1.getTransactionContent().getHash().toBase58());
	}
}
