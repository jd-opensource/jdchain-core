package test.com.jd.blockchain.consensus.bftsmart;

import static org.junit.Assert.*;

import org.junit.Test;

import com.jd.blockchain.consensus.bftsmart.service.BftsmartClientAuthencationService;

/**
 * 共识客户端 Id 分配测试；
 * 
 * @author huanghaiquan
 *
 */
public class BftsmartClientIdAllocationTest {

	@Test
	public void test() {
		int i = 0;
		int[] prevIdRange = BftsmartClientAuthencationService.computeClientIdRangeFromNode(i);
		System.out.println(String.format("节点[%s]的客户端Id范围 [%s —— %s).", i, prevIdRange[0], prevIdRange[1]));
		for (i = 1; i < 16; i++) {
			int[] nextIdRange = BftsmartClientAuthencationService.computeClientIdRangeFromNode(i);
			System.out.println(String.format("节点[%s]的客户端Id范围 [%s —— %s).", i, nextIdRange[0], nextIdRange[1]));
			
			assertTrue(prevIdRange[0] < prevIdRange[1]);
			assertTrue(prevIdRange[1] == nextIdRange[0]);
			prevIdRange = nextIdRange;
		}
		
		i = 999;
		int[] idRange = BftsmartClientAuthencationService.computeClientIdRangeFromNode(i);
		assertTrue(idRange[0] > 0);
		assertTrue(idRange[1] > 0);
		assertTrue(idRange[1] < Integer.MAX_VALUE);
		System.out.println(String.format("节点[%s]的客户端Id范围 [%s —— %s).", i, idRange[0], idRange[1]));

		System.out.println("整数最大值：" + Integer.MAX_VALUE);
		
	}

}
