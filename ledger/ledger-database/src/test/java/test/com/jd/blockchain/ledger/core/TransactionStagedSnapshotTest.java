/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: test.com.jd.blockchain.ledger.TransactionStagedSnapshotTest
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/8/30 上午10:49
 * Description:
 */
package test.com.jd.blockchain.ledger.core;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.service.classic.ClassicCryptoService;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.core.TransactionStagedSnapshot;

/**
 *
 * @author shaozhuguang
 * @create 2018/8/30
 * @since 1.0.0
 */

public class TransactionStagedSnapshotTest {

	private TransactionStagedSnapshot data;

	@Before
	public void initTransactionStagedSnapshot() {
		DataContractRegistry.register(LedgerDataSnapshot.class);
		data = new TransactionStagedSnapshot();
		data.setAdminAccountHash(ClassicCryptoService.SHA256.hash( "zhangsan".getBytes()));
		data.setContractAccountSetHash(ClassicCryptoService.SHA256.hash( "lisi".getBytes()));
		data.setDataAccountSetHash(ClassicCryptoService.SHA256.hash( "wangwu".getBytes()));
		data.setUserAccountSetHash(ClassicCryptoService.SHA256.hash( "zhaoliu".getBytes()));
	}

	@Test
	public void testSerialize_LedgerDataSnapshot() throws Exception {
		byte[] serialBytes = BinaryProtocol.encode(data, LedgerDataSnapshot.class);
		LedgerDataSnapshot resolvedData = BinaryProtocol.decode(serialBytes);
		System.out.println("------Assert start ------");
		assertEquals(resolvedData.getAdminAccountHash(), data.getAdminAccountHash());
		assertEquals(resolvedData.getContractAccountSetHash(), data.getContractAccountSetHash());
		assertEquals(resolvedData.getDataAccountSetHash(), data.getDataAccountSetHash());
		assertEquals(resolvedData.getUserAccountSetHash(), data.getUserAccountSetHash());
		System.out.println("------Assert OK ------");
	}
}