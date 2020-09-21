package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Random;

import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureFunction;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.crypto.service.classic.ClassicCryptoService;
import com.jd.blockchain.crypto.service.sm.SMCryptoService;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.CryptoConfig;
import com.jd.blockchain.ledger.core.LedgerDataset;
import com.jd.blockchain.ledger.core.LedgerEditor;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.LedgerTransactionalEditor;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;
import com.jd.blockchain.ledger.core.UserAccount;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.transaction.ConsensusParticipantData;
import com.jd.blockchain.transaction.LedgerInitData;
import com.jd.blockchain.transaction.TransactionService;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;
import com.jd.blockchain.utils.net.NetworkAddress;

public class LedgerTestUtils {
	
	public static final String LEDGER_KEY_PREFIX = "LDG://";

	public static final SignatureFunction ED25519_SIGN_FUNC = Crypto.getSignatureFunction("ED25519");

	public static final CryptoAlgorithm ED25519 = ED25519_SIGN_FUNC.getAlgorithm();

	private static final String[] SUPPORTED_PROVIDERS = { ClassicCryptoService.class.getName(),
			SMCryptoService.class.getName() };

	private static Random rand = new Random();

//	public static TransactionRequest createTxRequest_UserReg(HashDigest ledgerHash) {
//		BlockchainKeypair key = BlockchainKeyGenerator.getInstance().generate(ED25519);
//		return createTxRequest_UserReg(ledgerHash, key);
//	}

	public static LedgerInitSetting createLedgerInitSetting() {
		BlockchainKeypair[] partiKeys = new BlockchainKeypair[2];
		partiKeys[0] = BlockchainKeyGenerator.getInstance().generate();
		partiKeys[1] = BlockchainKeyGenerator.getInstance().generate();
		return createLedgerInitSetting(partiKeys);
	}
	
	public static LedgerInitSetting createLedgerInitSetting(CryptoSetting cryptoSetting) {
		BlockchainKeypair[] partiKeys = new BlockchainKeypair[2];
		partiKeys[0] = BlockchainKeyGenerator.getInstance().generate();
		partiKeys[1] = BlockchainKeyGenerator.getInstance().generate();
		return createLedgerInitSetting(cryptoSetting,partiKeys);
	}

	public static CryptoProvider[] getContextProviders() {
		CryptoProvider[] supportedProviders = new CryptoProvider[SUPPORTED_PROVIDERS.length];
		for (int i = 0; i < SUPPORTED_PROVIDERS.length; i++) {
			supportedProviders[i] = Crypto.getProvider(SUPPORTED_PROVIDERS[i]);
		}

		return supportedProviders;
	}
	
	public static LedgerInitSetting createLedgerInitSetting(BlockchainKeypair[] partiKeys) {
		CryptoSetting defCryptoSetting = createDefaultCryptoSetting();
		
		return createLedgerInitSetting(defCryptoSetting, partiKeys);
	}
	public static LedgerInitSetting createLedgerInitSetting(CryptoSetting cryptoSetting, BlockchainKeypair[] partiKeys) {
		LedgerInitData initSetting = new LedgerInitData();

		initSetting.setLedgerSeed(BytesUtils.toBytes("A Test Ledger seed!", "UTF-8"));
		initSetting.setCryptoSetting(cryptoSetting);
		ConsensusParticipantData[] parties = new ConsensusParticipantData[partiKeys.length];
		for (int i = 0; i < parties.length; i++) {
			parties[i] = new ConsensusParticipantData();
			parties[i].setId(0);
			parties[i].setName("Parti-" + i);
			parties[i].setPubKey(partiKeys[i].getPubKey());
			parties[i].setAddress(AddressEncoding.generateAddress(partiKeys[i].getPubKey()));
			parties[i].setHostAddress(new NetworkAddress("192.168.1." + (10 + i), 9000));
			parties[i].setParticipantState(ParticipantNodeState.CONSENSUS);

		}

		initSetting.setConsensusParticipants(parties);

		return initSetting;
	}

//	public static TransactionRequest createTxRequest_UserReg(BlockchainKeypair userKeypair, HashDigest ledgerHash, BlockchainKeypair... partiKeys) {
//		return createTxRequest_UserReg(userKeypair, ledgerHash, null, null);
//	}

	public static TransactionRequest createLedgerInitTxRequest_SHA256(BlockchainKeypair... participants) {
		TxBuilder txBuilder = new TxBuilder(null, ClassicAlgorithm.SHA256);

		for (BlockchainKeypair parti : participants) {
			txBuilder.users().register(parti.getIdentity());
		}

		// 202001202020L
//		long ts = System.currentTimeMillis();
//		long ts = 1580271132149L; //OK;
		long ts = 1580271176324L;
		System.out.printf("\r\n++++++ ts=%s ++++++\r\n", ts);
		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest(ts);
		for (BlockchainKeypair parti : participants) {
			txReqBuilder.signAsNode(parti);
		}

		return txReqBuilder.buildRequest();
	}

//	public static TransactionRequest createTxRequest_UserReg(HashDigest ledgerHash, BlockchainKeypair nodeKeypair,
//			BlockchainKeypair... signers) {
//		return createTxRequest_UserReg(BlockchainKeyGenerator.getInstance().generate(), ledgerHash, nodeKeypair,
//				signers);
//	}

	public static TransactionRequest createTxRequest_UserReg(BlockchainKeypair userKeypair, HashDigest ledgerHash,
			BlockchainKeypair nodeKeypair, BlockchainKeypair... signers) {
		return createTxRequest_UserReg_SHA256(userKeypair, ledgerHash, System.currentTimeMillis(), nodeKeypair, signers);
	}

	public static TransactionRequest createTxRequest_UserReg_SHA256(BlockchainKeypair userKeypair, HashDigest ledgerHash,
			long ts, BlockchainKeypair nodeKeypair, BlockchainKeypair... signers) {
		TxBuilder txBuilder = new TxBuilder(ledgerHash, ClassicAlgorithm.SHA256);

		txBuilder.users().register(userKeypair.getIdentity());

		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest(ts);
		if (signers != null) {
			for (BlockchainKeypair signer : signers) {
				txReqBuilder.signAsEndpoint(signer);
			}
		}
		if (nodeKeypair != null) {
			txReqBuilder.signAsNode(nodeKeypair);
		}
		return txReqBuilder.buildRequest();
	}

	public static TransactionRequest createTxRequest_DataAccountReg_SHA256(BlockchainKeypair dataAccountID,
			HashDigest ledgerHash, BlockchainKeypair nodeKeypair, BlockchainKeypair... signers) {
		TxBuilder txBuilder = new TxBuilder(ledgerHash, ClassicAlgorithm.SHA256);

		txBuilder.dataAccounts().register(dataAccountID.getIdentity());

		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest(202001202020L);
		if (signers != null) {
			for (BlockchainKeypair signer : signers) {
				txReqBuilder.signAsEndpoint(signer);
			}
		}
		if (nodeKeypair != null) {
			txReqBuilder.signAsNode(nodeKeypair);
		}

		return txReqBuilder.buildRequest();
	}

	public static TransactionRequest createTxRequest_DataAccountWrite_SHA256(Bytes dataAccountAddress, String key,
			String value, long version, HashDigest ledgerHash, BlockchainKeypair nodeKeypair,
			BlockchainKeypair... signers) {
		TxBuilder txBuilder = new TxBuilder(ledgerHash, ClassicAlgorithm.SHA256);

		txBuilder.dataAccount(dataAccountAddress).setText(key, value, version);

		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest(202001202020L);
		if (signers != null) {
			for (BlockchainKeypair signer : signers) {
				txReqBuilder.signAsEndpoint(signer);
			}
		}
		if (nodeKeypair != null) {
			txReqBuilder.signAsNode(nodeKeypair);
		}

		return txReqBuilder.buildRequest();
	}

	public static BlockchainKeypair createKeyPair(String pub, String priv) {
		PubKey pubKey = new PubKey(Base58Utils.decode(pub));
		PrivKey privKey = new PrivKey(Base58Utils.decode(priv));

		return new BlockchainKeypair(pubKey, privKey);
	}

	/**
	 * 创建一个写入到不存在账户的多操作交易；
	 * 
	 * @param userKeypair 要注册的用户key；
	 * @param ledgerHash  账本哈希；
	 * @param nodeKeypair 节点key；
	 * @param signers     签名者列表；
	 * @return
	 */
	public static TransactionRequest createTxRequest_MultiOPs_WithNotExistedDataAccount(BlockchainKeypair userKeypair,
			HashDigest ledgerHash, BlockchainKeypair nodeKeypair, BlockchainKeypair... signers) {
		return createTxRequest_MultiOPs_WithNotExistedDataAccount_SHA256(userKeypair, ledgerHash, System.currentTimeMillis(),
				nodeKeypair, signers);
	}

	/**
	 * 创建一个写入到不存在账户的多操作交易；
	 * 
	 * @param userKeypair 要注册的用户key；
	 * @param ledgerHash  账本哈希；
	 * @param nodeKeypair 节点key；
	 * @param signers     签名者列表；
	 * @return
	 */
	public static TransactionRequest createTxRequest_MultiOPs_WithNotExistedDataAccount_SHA256(BlockchainKeypair userKeypair,
			HashDigest ledgerHash, long ts, BlockchainKeypair nodeKeypair, BlockchainKeypair... signers) {
		TxBuilder txBuilder = new TxBuilder(ledgerHash, ClassicAlgorithm.SHA256);

		txBuilder.users().register(userKeypair.getIdentity());

		// 故意构建一个错误的
//		BlockchainKeypair testAccount = BlockchainKeyGenerator.getInstance().generate();
		BlockchainKeypair testAccount = createKeyPair("7VeRJRN2A1dwSAA6BNAM728aN12A3uwj1DVVVMzvJUP7Gnxm",
				"7VeRSBZNE6rNzkeRE3Sre2QbtNDKZNKqgzYhsvoY2QfKZenS");

		txBuilder.dataAccount(testAccount.getAddress()).setBytes("AA", "Value".getBytes(), 1);

		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest(ts);
		txReqBuilder.signAsEndpoint(nodeKeypair);
		if (nodeKeypair != null) {
			txReqBuilder.signAsNode(nodeKeypair);
		}

		return txReqBuilder.buildRequest();
	}
	
	public static HashDigest initLedger(MemoryKVStorage storage, BlockchainKeypair... partiKeys) {
		// 创建初始化配置；
		LedgerInitSetting initSetting = LedgerTestUtils.createLedgerInitSetting(partiKeys);

		// 创建账本；
		LedgerEditor ldgEdt = LedgerTransactionalEditor.createEditor(initSetting, LEDGER_KEY_PREFIX, storage, storage);

		TransactionRequest genesisTxReq = LedgerTestUtils.createLedgerInitTxRequest_SHA256(partiKeys);
		LedgerTransactionContext genisisTxCtx = ldgEdt.newTransaction(genesisTxReq);
		LedgerDataset ldgDS = genisisTxCtx.getDataset();

		for (int i = 0; i < partiKeys.length; i++) {
			UserAccount userAccount = ldgDS.getUserAccountSet().register(partiKeys[i].getAddress(),
					partiKeys[i].getPubKey());
			userAccount.setProperty("Name", "参与方-" + i, -1);
			userAccount.setProperty("Share", "" + (10 + i), -1);
		}

		TransactionResult tx = genisisTxCtx.commit(TransactionState.SUCCESS);

		assertEquals(genesisTxReq.getTransactionHash(), tx.getTransactionHash());
		assertEquals(0, tx.getBlockHeight());

		LedgerBlock block = ldgEdt.prepare();

		assertEquals(0, block.getHeight());
		assertNotNull(block.getHash());
		assertNull(block.getPreviousHash());

		// 创世区块的账本哈希为 null；
		assertNull(block.getLedgerHash());
		assertNotNull(block.getHash());

		// 提交数据，写入存储；
		ldgEdt.commit();

		HashDigest ledgerHash = block.getHash();
		return ledgerHash;
	}

	public static TransactionStagedSnapshot generateRandomSnapshot() {
		TransactionStagedSnapshot txDataSnapshot = new TransactionStagedSnapshot();
		txDataSnapshot.setAdminAccountHash(generateRandomHash());
		txDataSnapshot.setContractAccountSetHash(generateRandomHash());
		txDataSnapshot.setDataAccountSetHash(generateRandomHash());
		txDataSnapshot.setUserAccountSetHash(generateRandomHash());
		return txDataSnapshot;
	}

	public static HashDigest generateRandomHash() {
		byte[] data = new byte[64];
		rand.nextBytes(data);
		return Crypto.getHashFunction("SHA256").hash(data);
	}

	public static CryptoSetting createDefaultCryptoSetting() {

		CryptoProvider[] supportedProviders = getContextProviders();

		CryptoConfig cryptoSetting = new CryptoConfig();
		cryptoSetting.setSupportedProviders(supportedProviders);
		cryptoSetting.setAutoVerifyHash(true);
		cryptoSetting.setHashAlgorithm(ClassicAlgorithm.SHA256);
		return cryptoSetting;
	}

	private static class TxHandle implements TransactionService {

		private TransactionRequest txRequest;

		@Override
		public TransactionResponse process(TransactionRequest txRequest) {
			this.txRequest = txRequest;
			return null;
		}

	}

}
