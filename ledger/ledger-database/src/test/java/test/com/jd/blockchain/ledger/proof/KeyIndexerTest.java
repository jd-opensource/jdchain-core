package test.com.jd.blockchain.ledger.proof;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.jd.blockchain.ledger.proof.KeyIndexer;

public class KeyIndexerTest {

	@Test
	public void test() {
		int count = 1000000;
		long hash;
		
		byte[] data = new byte[128];
		
		hash = 3440206850337362796L;
		byte idx = KeyIndexer.index(hash, 7);
		assertTrue(idx >= 0 && idx < 16);
		
		Random rand = new Random();
		for (int i = 0; i < count; i++) {
			rand.nextBytes(data);
			hash = KeyIndexer.hash(data);
			for (int j = 0; j < 16; j++) {
				idx = KeyIndexer.index(hash, j);
				assertTrue(idx >= 0 && idx < 16);
			}
		}
	
	}

}
