package test.com.jd.blockchain.ledger.proof;

import java.util.Random;

import org.junit.Test;

import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.ledger.merkletree.MerkleSortTree;
import com.jd.blockchain.ledger.merkletree.TreeOptions;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.hash.MurmurHash3;

import test.com.jd.blockchain.ledger.core.LedgerTestUtils;

public class MerkleSortTreePerformanceTest {

	@Test
	public void test() {

		System.out.println("=========================================");
		testPerformace1(100, 1);
		testPerformace1(100, 10);
		testPerformace1(100, 100);
		testPerformace1(100, 1000);

		System.out.println("=========================================");
		testPerformace1(1000, 1);
		testPerformace1(1000, 10);
		testPerformace1(1000, 100);
		testPerformace1(1000, 1000);

		System.out.println("=========================================");
		testPerformace1(10000, 5);
		testPerformace1(10000, 10);

//		//--------------------
//		testPerformace1(100, 1);
//		testPerformace1(25, 4000);
//		testPerformace1(50, 4000);
//		testPerformace1(50, 2000);
//		testPerformace1(100, 2000);
//		testPerformace1(100, 1000);
//		testPerformace1(200, 1000);
//		testPerformace1(200, 500);
//		testPerformace1(400, 500);
	}

	private void testPerformace1(int round, int count) {
		System.out.printf("------------- Performance test: MerkleSortTree --------------\r\n", round, count);

		TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(ClassicAlgorithm.SHA256.code());
		Bytes prefix = Bytes.fromString(LedgerTestUtils.LEDGER_KEY_PREFIX);
		MemoryKVStorage storage = new MemoryKVStorage();

		Random rand = new Random();
		byte[] value = new byte[128];
		rand.nextBytes(value);

		long startTs = System.currentTimeMillis();

		MerkleSortTree<byte[]> merkleTree = MerkleSortTree.createBytesTree(options, prefix, storage);
		long key;
		for (int r = 0; r < round; r++) {
			for (int i = 0; i < count; i++) {
				key = ((long) r << 32) | i;
				MurmurHash3.murmurhash3_x64_64_1(value, 0, value.length, 10);
				merkleTree.set(key, value);
			}
			merkleTree.commit();
		}

		long elapsedTs = System.currentTimeMillis() - startTs;

		long totalCount = count * round;
		double tps = round * 1000.0D / elapsedTs;
		double kps = round * count * 1000.0D / elapsedTs;
		System.out.printf("--[Performance]:: TotalKeys=%s; Round=%s; Count=%s; Times=%sms; TPS=%.2f; KPS=%.2f\r\n\r\n",
				totalCount, round, count, elapsedTs, tps, kps);
	}

}
