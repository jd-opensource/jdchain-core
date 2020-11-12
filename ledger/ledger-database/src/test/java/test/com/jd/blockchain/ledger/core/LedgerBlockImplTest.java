/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: test.com.jd.blockchain.ledger.LedgerBlockImplTest
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/8/30 上午10:45
 * Description:
 */
package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.service.classic.ClassicCryptoService;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.core.LedgerBlockData;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;

/**
 *
 * @author shaozhuguang
 * @create 2018/8/30
 * @since 1.0.0
 */

public class LedgerBlockImplTest {

	private LedgerBlockData data;

	@Before
	public void initLedgerBlockImpl() {
		DataContractRegistry.register(LedgerBlock.class);
		DataContractRegistry.register(LedgerDataSnapshot.class);
		long height = 9999L;
		HashDigest ledgerHash = ClassicCryptoService.SHA256.hash( "zhangsan".getBytes());
		HashDigest previousHash = ClassicCryptoService.SHA256.hash( "lisi".getBytes());
		data = new LedgerBlockData(height, ledgerHash, previousHash);
		data.setHash(ClassicCryptoService.SHA256.hash( "wangwu".getBytes()));
		data.setTransactionSetHash(ClassicCryptoService.SHA256.hash( "zhaoliu".getBytes()));

		// 设置LedgerDataSnapshot相关属性
		data.setAdminAccountHash(ClassicCryptoService.SHA256.hash( "jd1".getBytes()));
		data.setDataAccountSetHash(ClassicCryptoService.SHA256.hash( "jd2".getBytes()));
		data.setUserAccountSetHash(ClassicCryptoService.SHA256.hash( "jd3".getBytes()));
		data.setContractAccountSetHash(ClassicCryptoService.SHA256.hash( "jd4".getBytes()));

	}

	@Test
	public void testSerialize_LedgerBlock() throws Exception {
		byte[] serialBytes = BinaryProtocol.encode(data, LedgerBlock.class);
		LedgerBlock resolvedData1 = BinaryProtocol.decode(serialBytes);
		System.out.println("------Assert start ------");
		assertEquals(data.getHash(), resolvedData1.getHash());
		assertEquals(data.getHeight(), resolvedData1.getHeight());
		assertEquals(data.getLedgerHash(), resolvedData1.getLedgerHash());
		assertEquals(data.getPreviousHash(), resolvedData1.getPreviousHash());
		assertEquals(data.getTransactionSetHash(), resolvedData1.getTransactionSetHash());
		assertEquals(data.getAdminAccountHash(), resolvedData1.getAdminAccountHash());
		assertEquals(data.getContractAccountSetHash(), resolvedData1.getContractAccountSetHash());
		assertEquals(data.getDataAccountSetHash(), resolvedData1.getDataAccountSetHash());
		assertEquals(data.getUserAccountSetHash(), resolvedData1.getUserAccountSetHash());
		System.out.println("------Assert OK ------");
	}

	// notice: LedgerBlock interface has more field info than LedgerDataSnapshot
	// interface, so cannot deserialize LedgerBlock
	// with LedgerDataSnapshot encode
	// @Test
	// public void testSerialize_LedgerDataSnapshot() throws Exception {
	// byte[] serialBytes = BinaryEncodingUtils.encode(data,
	// LedgerDataSnapshot.class);
	// LedgerDataSnapshot resolvedData = BinaryEncodingUtils.decode(serialBytes,
	// null,
	// LedgerBlockData.class);
	// System.out.println("------Assert start ------");
	// assertEquals(resolvedData.getAdminAccountHash(), data.getAdminAccountHash());
	// assertEquals(resolvedData.getAdminAccountHash(), data.getAdminAccountHash());
	// assertEquals(resolvedData.getContractAccountSetHash(),
	// data.getContractAccountSetHash());
	// assertEquals(resolvedData.getDataAccountSetHash(),
	// data.getDataAccountSetHash());
	// assertEquals(resolvedData.getUserAccountSetHash(),
	// data.getUserAccountSetHash());
	// System.out.println("------Assert OK ------");
	// }

	@Test
	public void testSerialize_LedgerDataSnapshot() throws Exception {
		TransactionStagedSnapshot transactionStagedSnapshot = new TransactionStagedSnapshot();
		
		
		HashDigest admin = ClassicCryptoService.SHA256.hash( "alice".getBytes());
		HashDigest contract = ClassicCryptoService.SHA256.hash( "bob".getBytes());
		HashDigest data = ClassicCryptoService.SHA256.hash( "jerry".getBytes());
		HashDigest user = ClassicCryptoService.SHA256.hash( "tom".getBytes());

		transactionStagedSnapshot.setAdminAccountHash(admin);
		transactionStagedSnapshot.setContractAccountSetHash(contract);
		transactionStagedSnapshot.setDataAccountSetHash(data);
		transactionStagedSnapshot.setUserAccountSetHash(user);

		byte[] serialBytes = BinaryProtocol.encode(transactionStagedSnapshot, LedgerDataSnapshot.class);
		LedgerDataSnapshot resolvedData = BinaryProtocol.decode(serialBytes);

		// verify start
		assertEquals(resolvedData.getAdminAccountHash(), transactionStagedSnapshot.getAdminAccountHash());
		assertEquals(resolvedData.getContractAccountSetHash(), transactionStagedSnapshot.getContractAccountSetHash());
		assertEquals(resolvedData.getDataAccountSetHash(), transactionStagedSnapshot.getDataAccountSetHash());
		assertEquals(resolvedData.getUserAccountSetHash(), transactionStagedSnapshot.getUserAccountSetHash());
		// verify succeed

	}

}