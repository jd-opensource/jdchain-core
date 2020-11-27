package test.com.jd.blockchain.consensus.bftsmart;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder;

public class ComputeBFTNumberTest {

	@Test
	public void test() {
		// 预期 1、2、3 节点数量的容错都为 0；
		int f = BftsmartConsensusSettingsBuilder.computeBFTNumber(1);
		assertEquals(0, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(2);
		assertEquals(0, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(3);
		assertEquals(0, f);

		// 预期 4、5、6 节点数量的容错都为 1；
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(4);
		assertEquals(1, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(5);
		assertEquals(1, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(6);
		assertEquals(1, f);
		
		// 预期 7、8、9 节点数量的容错都为 2；
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(7);
		assertEquals(2, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(8);
		assertEquals(2, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(9);
		assertEquals(2, f);
		

		// 预期 10、11、12 节点数量的容错都为 3；
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(10);
		assertEquals(3, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(11);
		assertEquals(3, f);
		f = BftsmartConsensusSettingsBuilder.computeBFTNumber(12);
		assertEquals(3, f);
		
		// 一般化，输入大于等于 3*f+1 小于 3*(f+1)+1 之间的数，能够返回 f ；
		Random random = new Random();
		for (int i = 0; i < 100; i++) {
			f = random.nextInt(1000000);
			int N = 3 * f + 1;
			int N1 = 3 * (f+1) + 1;
			for (int n = N; n < N1; n++) {
				int f1 = BftsmartConsensusSettingsBuilder.computeBFTNumber(n);
				assertEquals(f, f1);
			}
		}
		
		//传递小于 1 的值时预期出错；
		Exception error = null;
		try {
			BftsmartConsensusSettingsBuilder.computeBFTNumber(0);
		} catch (IllegalArgumentException e) {
			error = e;
		}
		assertNotNull(error);
		
		error = null;
		try {
			BftsmartConsensusSettingsBuilder.computeBFTNumber(-1);
		} catch (IllegalArgumentException e) {
			error = e;
		}
		assertNotNull(error);
	}

}
