/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: test.com.jd.blockchain.ledger.data.LedgerTransactionImplTest
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/8/30 上午9:48
 * Description:
 */
package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureDigest;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.ledger.BlockchainKeyGenerator;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.HashObject;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.core.LedgerTransactionData;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;
import com.jd.blockchain.transaction.BlockchainOperationFactory;
import com.jd.blockchain.transaction.DigitalSignatureBlob;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.transaction.TxContentBlob;
import com.jd.blockchain.transaction.TxRequestMessage;

/**
 *
 * @author shaozhuguang
 * @create 2018/8/30
 * @since 1.0.0
 */

public class LedgerTransactionDataTest {

	private LedgerTransactionData data;
	
	private TransactionRequest txRequest;

	@Before
	public void initLedgerTransactionImpl() throws Exception {
		DataContractRegistry.register(LedgerTransaction.class);
		DataContractRegistry.register(LedgerDataSnapshot.class);
		DataContractRegistry.register(HashObject.class);
		DataContractRegistry.register(DataAccountKVSetOperation.class);

		txRequest = initTxRequestMessage(ClassicAlgorithm.SHA256);

		long blockHeight = 9986L;
		TransactionStagedSnapshot snapshot = initTransactionStagedSnapshot();
		data = new LedgerTransactionData(blockHeight, txRequest, TransactionState.SUCCESS,
				snapshot, null);

		HashDigest hash = new HashDigest(ClassicAlgorithm.SHA256, "zhangsan".getBytes());
		HashDigest adminAccountHash = new HashDigest(ClassicAlgorithm.SHA256, "lisi".getBytes());
		HashDigest userAccountSetHash = new HashDigest(ClassicAlgorithm.SHA256, "wangwu".getBytes());
		HashDigest dataAccountSetHash = new HashDigest(ClassicAlgorithm.SHA256, "zhaoliu".getBytes());
		HashDigest contractAccountSetHash = new HashDigest(ClassicAlgorithm.SHA256, "sunqi".getBytes());

		snapshot.setAdminAccountHash(adminAccountHash);
		snapshot.setUserAccountSetHash(userAccountSetHash);
		snapshot.setDataAccountSetHash(dataAccountSetHash);
		snapshot.setContractAccountSetHash(contractAccountSetHash);
	}

	@Test
	public void testSerialize_LedgerTransaction() throws Exception {
		byte[] serialBytes = BinaryProtocol.encode(data, LedgerTransaction.class);
		LedgerTransaction resolvedData = BinaryProtocol.decode(serialBytes);

		System.out.println("------Assert start ------");
		assertEquals(resolvedData.getExecutionState(), data.getExecutionState());
		assertEquals(resolvedData.getBlockHeight(), data.getBlockHeight());

		assertEquals(resolvedData.getDataSnapshot().getAdminAccountHash(), data.getDataSnapshot().getAdminAccountHash());
		assertEquals(resolvedData.getDataSnapshot().getContractAccountSetHash(), data.getDataSnapshot().getContractAccountSetHash());
		assertEquals(resolvedData.getDataSnapshot().getDataAccountSetHash(), data.getDataSnapshot().getDataAccountSetHash());
		assertEquals(resolvedData.getDataSnapshot().getUserAccountSetHash(), data.getDataSnapshot().getUserAccountSetHash());
		assertEquals(resolvedData.getExecutionState(), data.getExecutionState());
//		assertEquals(resolvedData.getHash(), data.getHash());
		assertEquals(resolvedData.getBlockHeight(), data.getBlockHeight());

		// EndpointSignatures 验证
//		DigitalSignature[] dataEndpointSignatures = data.getEndpointSignatures();
//		DigitalSignature[] resolvedEndpointSignatures = resolvedData.getEndpointSignatures();
//		for (int i = 0; i < dataEndpointSignatures.length; i++) {
//			assertEquals(dataEndpointSignatures[i].getPubKey(), resolvedEndpointSignatures[i].getPubKey());
//			assertEquals(dataEndpointSignatures[i].getDigest(), resolvedEndpointSignatures[i].getDigest());
//		}

		// NodeSignatures 验证
//		DigitalSignature[] dataNodeSignatures = data.getNodeSignatures();
//		DigitalSignature[] resolvedNodeSignatures = resolvedData.getNodeSignatures();
//		for (int i = 0; i < dataNodeSignatures.length; i++) {
//			assertEquals(dataNodeSignatures[i].getPubKey(), resolvedNodeSignatures[i].getPubKey());
//			assertEquals(dataNodeSignatures[i].getDigest(), resolvedNodeSignatures[i].getDigest());
//		}
//
//		assertEqual(data.getTransactionContent(), resolvedData.getTransactionContent());
		System.out.println("------Assert OK ------");
	}

	@Test
	public void testSerialize_Transaction() throws Exception {
		byte[] serialBytes = BinaryProtocol.encode(txRequest, TransactionRequest.class);
		TransactionRequest resolvedData = BinaryProtocol.decode(serialBytes);

		System.out.println("------Assert start ------");
		

		// EndpointSignatures 验证
		DigitalSignature[] dataEndpointSignatures = txRequest.getEndpointSignatures();
		DigitalSignature[] resolvedEndpointSignatures = resolvedData.getEndpointSignatures();
		for (int i = 0; i < dataEndpointSignatures.length; i++) {
			assertEquals(dataEndpointSignatures[i].getPubKey(), resolvedEndpointSignatures[i].getPubKey());
			assertEquals(dataEndpointSignatures[i].getDigest(), resolvedEndpointSignatures[i].getDigest());
		}

		// NodeSignatures 验证
		DigitalSignature[] dataNodeSignatures = txRequest.getNodeSignatures();
		DigitalSignature[] resolvedNodeSignatures = resolvedData.getNodeSignatures();
		for (int i = 0; i < dataNodeSignatures.length; i++) {
			assertEquals(dataNodeSignatures[i].getPubKey(), resolvedNodeSignatures[i].getPubKey());
			assertEquals(dataNodeSignatures[i].getDigest(), resolvedNodeSignatures[i].getDigest());
		}

		assertTransactionEqual(txRequest.getTransactionContent(), resolvedData.getTransactionContent());
		System.out.println("------Assert OK ------");
	}


	private void assertTransactionEqual(TransactionContent dataTxContent, TransactionContent resolvedTxContent) {
		assertEquals(dataTxContent.getLedgerHash(), resolvedTxContent.getLedgerHash());

		byte[] txBytes1 = BinaryProtocol.encode(dataTxContent, TransactionContent.class);
		byte[] txBytes2 = BinaryProtocol.encode(resolvedTxContent, TransactionContent.class);
		assertArrayEquals(txBytes1, txBytes2);

//		assertEquals(dataTxContent.getHash(), resolvedTxContent.getHash());

		// assertEquals(dataTxContent.getSequenceNumber(),
		// resolvedTxContent.getSequenceNumber());
		// assertEquals(dataTxContent.getSubjectAccount(),
		// resolvedTxContent.getSubjectAccount());
	}

	private TransactionStagedSnapshot initTransactionStagedSnapshot() {
		TransactionStagedSnapshot transactionStagedSnapshot = new TransactionStagedSnapshot();
		transactionStagedSnapshot.setAdminAccountHash(new HashDigest(ClassicAlgorithm.SHA256, "zhangsan".getBytes()));
		transactionStagedSnapshot.setContractAccountSetHash(new HashDigest(ClassicAlgorithm.SHA256, "lisi".getBytes()));
		transactionStagedSnapshot.setDataAccountSetHash(new HashDigest(ClassicAlgorithm.SHA256, "wangwu".getBytes()));
		transactionStagedSnapshot.setUserAccountSetHash(new HashDigest(ClassicAlgorithm.SHA256, "zhaoliu".getBytes()));
		return transactionStagedSnapshot;
	}

	private TxRequestMessage initTxRequestMessage(CryptoAlgorithm hashAlgorithm) throws Exception {
		TransactionContent txContent = initTransactionContent();
		HashDigest txHash = TxBuilder.computeTxContentHash(hashAlgorithm, txContent);
		TxRequestMessage txRequestMessage = new TxRequestMessage(txHash, txContent);

		SignatureDigest digest1 = new SignatureDigest(ClassicAlgorithm.ED25519, "zhangsan".getBytes());
		SignatureDigest digest2 = new SignatureDigest(ClassicAlgorithm.ED25519, "lisi".getBytes());
		DigitalSignatureBlob endPoint1 = new DigitalSignatureBlob(
				new PubKey(ClassicAlgorithm.ED25519, "jd1.com".getBytes()), digest1);
		DigitalSignatureBlob endPoint2 = new DigitalSignatureBlob(
				new PubKey(ClassicAlgorithm.ED25519, "jd2.com".getBytes()), digest2);
		txRequestMessage.addEndpointSignatures(endPoint1);
		txRequestMessage.addEndpointSignatures(endPoint2);

		SignatureDigest digest3 = new SignatureDigest(ClassicAlgorithm.ED25519, "wangwu".getBytes());
		SignatureDigest digest4 = new SignatureDigest(ClassicAlgorithm.ED25519, "zhaoliu".getBytes());
		DigitalSignatureBlob node1 = new DigitalSignatureBlob(
				new PubKey(ClassicAlgorithm.ED25519, "jd3.com".getBytes()), digest3);
		DigitalSignatureBlob node2 = new DigitalSignatureBlob(
				new PubKey(ClassicAlgorithm.ED25519, "jd4.com".getBytes()), digest4);
		txRequestMessage.addNodeSignatures(node1);
		txRequestMessage.addNodeSignatures(node2);

		return txRequestMessage;
	}

	private TransactionContent initTransactionContent() throws Exception {
		TxContentBlob contentBlob = null;
		BlockchainKeypair id = BlockchainKeyGenerator.getInstance().generate(ClassicAlgorithm.ED25519);
		HashDigest ledgerHash = Crypto.getHashFunction("SHA256").hash(UUID.randomUUID().toString().getBytes("UTF-8"));
		BlockchainOperationFactory opFactory = new BlockchainOperationFactory();
		contentBlob = new TxContentBlob(ledgerHash);
		// contentBlob.setSubjectAccount(id.getAddress());
		// contentBlob.setSequenceNumber(1);
		DataAccountKVSetOperation kvsetOP = opFactory.dataAccount(id.getAddress()).setText("Name", "AAA", -1)
				.getOperation();
		contentBlob.addOperation(kvsetOP);
		return contentBlob;
	}
}