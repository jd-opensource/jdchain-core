package test.com.jd.blockchain.consensus.bftsmart;

import static org.junit.Assert.*;

import org.junit.Test;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider;

public class BftsmartConsensusTest {

	@Test
	public void testNormal() {
		
		
		ConsensusProvider csProvider = new BftsmartConsensusProvider();
		
		
		
		
		csProvider.getSettingsFactory().getConsensusSettingsBuilder();
	}

}
