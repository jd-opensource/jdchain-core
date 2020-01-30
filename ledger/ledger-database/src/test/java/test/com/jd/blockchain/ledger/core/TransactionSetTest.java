package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainKeyGenerator;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.BytesDataList;
import com.jd.blockchain.ledger.BytesValueList;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountKVSetOperation.KVWriteEntry;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.ParticipantRegisterOperation;
import com.jd.blockchain.ledger.ParticipantStateUpdateOperation;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionRequestBuilder;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.LedgerTransactionData;
import com.jd.blockchain.ledger.core.TransactionQuery;
import com.jd.blockchain.ledger.core.TransactionSet;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;
import com.jd.blockchain.storage.service.utils.BufferedKVStorage;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;

public class TransactionSetTest {

	private static final String keyPrefix = "LDG://3A3dP4";

	private Random rand = new Random();

	static {
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(DataAccountRegisterOperation.class);
		DataContractRegistry.register(DataAccountKVSetOperation.class);
		DataContractRegistry.register(ContractCodeDeployOperation.class);
		DataContractRegistry.register(ContractEventSendOperation.class);
		DataContractRegistry.register(ParticipantRegisterOperation.class);
		DataContractRegistry.register(ParticipantStateUpdateOperation.class);
	}

	@Test
	public void test() {

		CryptoSetting defCryptoSetting = LedgerTestUtils.createDefaultCryptoSetting();

		MemoryKVStorage testStorage = new MemoryKVStorage();

		// Create a new TransactionSet, it's empty;
		TransactionSet txset = new TransactionSet(defCryptoSetting, keyPrefix, testStorage, testStorage);
		assertTrue(txset.isUpdated());
		assertFalse(txset.isReadonly());
		assertNull(txset.getRootHash());

		// Build transaction request;
		HashDigest ledgerHash = LedgerTestUtils.generateRandomHash();
		TxBuilder txBuilder = new TxBuilder(ledgerHash);

		BlockchainKeypair userKey = BlockchainKeyGenerator.getInstance().generate();
		UserRegisterOperation userRegOp = txBuilder.users().register(userKey.getIdentity());

		BlockchainKeypair dataKey = BlockchainKeyGenerator.getInstance().generate();
		DataAccountRegisterOperation dataAccRegOp = txBuilder.dataAccounts().register(dataKey.getIdentity());

		DataAccountKVSetOperation kvsetOP = txBuilder.dataAccount(dataKey.getAddress()).setText("A", "Value_A_0", -1)
				.setText("B", "Value_B_0", -1).getOperation();

		byte[] chainCode = new byte[128];
		rand.nextBytes(chainCode);
		BlockchainKeypair contractKey = BlockchainKeyGenerator.getInstance().generate();
		ContractCodeDeployOperation contractDplOP = txBuilder.contracts().deploy(contractKey.getIdentity(), chainCode);

		ContractEventSendOperation contractEvtSendOP = txBuilder.contractEvents().send(contractKey.getAddress(), "test",
				BytesDataList.singleText("TestContractArgs"));

		TransactionRequestBuilder txReqBuilder = txBuilder.prepareRequest();

		BlockchainKeypair sponsorKey = BlockchainKeyGenerator.getInstance().generate();
		txReqBuilder.signAsEndpoint(sponsorKey);
		BlockchainKeypair gatewayKey = BlockchainKeyGenerator.getInstance().generate();
		txReqBuilder.signAsNode(gatewayKey);

		TransactionRequest txReq = txReqBuilder.buildRequest();

		TransactionStagedSnapshot txSnapshot = new TransactionStagedSnapshot();
		HashDigest adminAccountHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setAdminAccountHash(adminAccountHash);
		HashDigest userAccountSetHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setUserAccountSetHash(userAccountSetHash);
		HashDigest dataAccountSetHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setDataAccountSetHash(dataAccountSetHash);
		HashDigest contractAccountSetHash = LedgerTestUtils.generateRandomHash();
		txSnapshot.setContractAccountSetHash(contractAccountSetHash);

		long blockHeight = 8922L;
		LedgerTransactionData tx = new LedgerTransactionData(blockHeight, txReq, TransactionState.SUCCESS, txSnapshot);
		txset.add(tx);

		assertTrue(txset.isUpdated());

		txset.commit();
		HashDigest txsetRootHash = txset.getRootHash();
		assertNotNull(txsetRootHash);
		assertEquals(1, txset.getTotalCount());
		assertEquals(blockHeight, tx.getBlockHeight());
		assertEquals(ledgerHash, tx.getTransactionContent().getLedgerHash());
		assertEquals(5, tx.getTransactionContent().getOperations().length);

		// Reload ;
		TransactionQuery reloadTxset = new TransactionSet(txsetRootHash, defCryptoSetting, keyPrefix, testStorage,
				testStorage, true);

		assertEquals(1, reloadTxset.getTotalCount());

		HashDigest txCtnHash = txReq.getTransactionContent().getHash();
		LedgerTransaction reloadTx = reloadTxset.get(txCtnHash);
		assertNotNull(reloadTx);

		assertEquals(tx.getHash(), reloadTx.getHash());
		assertEquals(tx.getBlockHeight(), reloadTx.getBlockHeight());
		assertEquals(tx.getAdminAccountHash(), reloadTx.getAdminAccountHash());
		assertEquals(tx.getContractAccountSetHash(), reloadTx.getContractAccountSetHash());
		assertEquals(tx.getDataAccountSetHash(), reloadTx.getDataAccountSetHash());
		assertEquals(tx.getUserAccountSetHash(), reloadTx.getUserAccountSetHash());
		assertEquals(TransactionState.SUCCESS, reloadTx.getExecutionState());

		DigitalSignature[] expEndpointSignatures = tx.getEndpointSignatures();
		DigitalSignature[] actualEndpointSignatures = reloadTx.getEndpointSignatures();
		assertEquals(expEndpointSignatures.length, actualEndpointSignatures.length);
		for (int i = 0; i < actualEndpointSignatures.length; i++) {
			assertEquals(expEndpointSignatures[i].getPubKey(), actualEndpointSignatures[i].getPubKey());
			assertEquals(expEndpointSignatures[i].getDigest(), actualEndpointSignatures[i].getDigest());
		}

		DigitalSignature[] expNodeSignatures = tx.getNodeSignatures();
		DigitalSignature[] actualNodeSignatures = reloadTx.getNodeSignatures();
		assertEquals(expNodeSignatures.length, actualNodeSignatures.length);
		for (int i = 0; i < actualNodeSignatures.length; i++) {
			assertEquals(expNodeSignatures[i].getPubKey(), actualNodeSignatures[i].getPubKey());
			assertEquals(expNodeSignatures[i].getDigest(), actualNodeSignatures[i].getDigest());
		}

		Operation[] expOperations = tx.getTransactionContent().getOperations();
		Operation[] actualOperations = reloadTx.getTransactionContent().getOperations();
		assertEquals(expOperations.length, actualOperations.length);
		assertEquals(5, actualOperations.length);
		assertTrue(actualOperations[0] instanceof UserRegisterOperation);
		assertTrue(actualOperations[1] instanceof DataAccountRegisterOperation);
		assertTrue(actualOperations[2] instanceof DataAccountKVSetOperation);
		assertTrue(actualOperations[3] instanceof ContractCodeDeployOperation);
		assertTrue(actualOperations[4] instanceof ContractEventSendOperation);

		UserRegisterOperation actualUserRegOp = (UserRegisterOperation) actualOperations[0];
		assertEquals(userRegOp.getUserID().getAddress(), actualUserRegOp.getUserID().getAddress());
		assertEquals(userRegOp.getUserID().getPubKey(), actualUserRegOp.getUserID().getPubKey());

		DataAccountRegisterOperation actualDataAccRegOp = (DataAccountRegisterOperation) actualOperations[1];
		assertEquals(dataAccRegOp.getAccountID().getAddress(), actualDataAccRegOp.getAccountID().getAddress());
		assertEquals(dataAccRegOp.getAccountID().getPubKey(), actualDataAccRegOp.getAccountID().getPubKey());

		DataAccountKVSetOperation actualKVSetOp = (DataAccountKVSetOperation) actualOperations[2];
		assertEquals(kvsetOP.getAccountAddress(), actualKVSetOp.getAccountAddress());

		KVWriteEntry[] expKVWriteSet = kvsetOP.getWriteSet();
		KVWriteEntry[] acutualKVWriteSet = actualKVSetOp.getWriteSet();
		assertEquals(expKVWriteSet.length, acutualKVWriteSet.length);
		for (int i = 0; i < acutualKVWriteSet.length; i++) {
			assertEquals(expKVWriteSet[i].getKey(), acutualKVWriteSet[i].getKey());
			assertEquals(expKVWriteSet[i].getExpectedVersion(), acutualKVWriteSet[i].getExpectedVersion());
			assertTrue(BytesUtils.equals(expKVWriteSet[i].getValue().getBytes().toBytes(),
					acutualKVWriteSet[i].getValue().getBytes().toBytes()));
		}

		ContractCodeDeployOperation actualContractDplOp = (ContractCodeDeployOperation) actualOperations[3];
		assertEquals(contractDplOP.getContractID().getAddress(), actualContractDplOp.getContractID().getAddress());
		assertEquals(contractDplOP.getContractID().getPubKey(), actualContractDplOp.getContractID().getPubKey());
		assertTrue(BytesUtils.equals(contractDplOP.getChainCode(), actualContractDplOp.getChainCode()));

		ContractEventSendOperation actualContractEvtSendOp = (ContractEventSendOperation) actualOperations[4];
		assertEquals(contractEvtSendOP.getContractAddress(), actualContractEvtSendOp.getContractAddress());
		assertEquals(contractEvtSendOP.getEvent(), actualContractEvtSendOp.getEvent());
		assertEquals("test", actualContractEvtSendOp.getEvent());

		byte[] expectedBytes = BinaryProtocol.encode(contractEvtSendOP.getArgs(), BytesValueList.class);
		byte[] actualBytes = BinaryProtocol.encode(actualContractEvtSendOp.getArgs(), BytesValueList.class);
		assertArrayEquals(expectedBytes, actualBytes);

		expectedBytes = BinaryProtocol.encode(BytesDataList.singleText("TestContractArgs"), BytesValueList.class);
		actualBytes = BinaryProtocol.encode(actualContractEvtSendOp.getArgs(), BytesValueList.class);
		assertArrayEquals(expectedBytes, actualBytes);
	}

	/**
	 * 利用一个随机出现的错误中采用的特殊值来验证正确性；
	 */
	@Test
	public void testSpecialCase_1() {
		CryptoSetting defCryptoSetting = LedgerTestUtils.createDefaultCryptoSetting();

		MemoryKVStorage testStorage = new MemoryKVStorage();

		BufferedKVStorage bufferStorage = new BufferedKVStorage(testStorage, testStorage, false);

		// Create a new TransactionSet, it's empty;
		TransactionSet txset = new TransactionSet(defCryptoSetting, keyPrefix, bufferStorage, bufferStorage);
		assertTrue(txset.isUpdated());
		assertFalse(txset.isReadonly());
		assertNull(txset.getRootHash());

		HashDigest ledgerHash = new HashDigest(Base58Utils.decode("j5iF5xJ7KN4kjRrhD3EUKVSPmHz2bExxp3h9avqxcnnzch"));
		assertEquals("j5iF5xJ7KN4kjRrhD3EUKVSPmHz2bExxp3h9avqxcnnzch", ledgerHash.toBase58());

		BlockchainKeypair parti0 = LedgerTestUtils.createKeyPair("7VeRLBwqTAz8oRazEazeaEfqei46sk2FzvBgyHMUBJvrUEGT",
				"7VeRUm27GbrsX9HbQSZguChLp24HZYub6s5FJ7FjBht8BmbA");

		BlockchainKeypair userKeypair1 = LedgerTestUtils.createKeyPair(
				"7VeRKf3GFLFcBfzvtzmtyMXEoX2HYGEJ4j7CmHcnRV99W5Dp", "7VeRYQjeAaQY5Po8MMtmGNHA2SniqLXmJaZwBS5K8zTtMAU1");
		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_UserReg(userKeypair1, ledgerHash, 1580315317127L,
				parti0, parti0);
//		TransactionRequest transactionRequest1 = LedgerTestUtils.createTxRequest_UserReg(userKeypair1, ledgerHash, 202001202020L,
//				parti0, parti0);
		System.out.printf("\r\n ===||=== transactionRequest1.getTransactionContent().getHash()=[%s]\r\n",
				transactionRequest1.getTransactionContent().getHash().toBase58());
//		assertEquals("j5sXmpcomtM2QMUNWeQWsF8bNFFnyeXoCjVAekEeLSscgY", transactionRequest1.getTransactionContent().getHash().toBase58());
		assertEquals("j5wPGKT5CUzwi8j6VfCWaP2p9YZ6WVWtMANp9HbHWzvhgG", transactionRequest1.getTransactionContent().getHash().toBase58());

		TransactionStagedSnapshot txSnapshot = new TransactionStagedSnapshot();
		txSnapshot.setAdminAccountHash(
				new HashDigest(Base58Utils.decode("j5taeK6cpmJGcn8QbEYCqadna6s7NDSheDTK6NJdU4mFhh")));
		txSnapshot.setUserAccountSetHash(
				new HashDigest(Base58Utils.decode("j5oQDSob92mCoGSHtrXa9soqgAtMyjwfRMt2kj7igXXJrP")));

		LedgerTransaction tx = new LedgerTransactionData(1, transactionRequest1, TransactionState.SUCCESS, txSnapshot);
		txset.add(tx);

		LedgerTransaction tx_query = txset.get(transactionRequest1.getTransactionContent().getHash());
		assertNotNull(tx_query);

		txset.commit();
		bufferStorage.commit();

		tx_query = txset.get(transactionRequest1.getTransactionContent().getHash());
		assertNotNull(tx_query);

		HashDigest txsetRootHash = txset.getRootHash();

		txset = new TransactionSet(txsetRootHash, defCryptoSetting, keyPrefix, testStorage, testStorage, false);
		tx_query = txset.get(transactionRequest1.getTransactionContent().getHash());
		assertNotNull(tx_query);
	}


}
