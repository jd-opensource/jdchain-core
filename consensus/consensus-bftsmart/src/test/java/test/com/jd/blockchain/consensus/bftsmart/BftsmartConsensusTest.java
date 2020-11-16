package test.com.jd.blockchain.consensus.bftsmart;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.utils.PropertiesUtils;

public class BftsmartConsensusTest {

	/**
	 * 标准功能用例：建立4个副本节点的共识网络，可以正常地达成进行共识；
	 * <p>
	 * 1. 建立 4 个副本节点的共识网络，启动全部的节点；<br>
	 * 2. 建立不少于 1 个共识客户端连接到共识网络；<br>
	 * 3. 共识客户端并发地提交共识消息，每个副本节点都能得到一致的消息队列；<br>
	 * 4. 副本节点对每一条消息都返回相同的响应，共识客户端能够得到正确的回复结果；<br>
	 * @throws IOException 
	 */
	@Test
	public void testNormal() throws IOException {
		final int N = 4;
		Replica[] replicas = new Replica[N];
		for (int i = 0; i < replicas.length; i++) {
			ReplicaNode rp = new ReplicaNode(i);
			AsymmetricKeypair kp = Crypto.getSignatureFunction(ClassicAlgorithm.ED25519).generateKeypair();
			rp.setKey(kp);
			rp.setName("节点[" + i + "]");
			replicas[i] = rp;
		}
		
		Properties csProperties = PropertiesUtils.loadProperties("classpath:bftsmart.config", "UTF-8");

		ConsensusProvider csProvider = new BftsmartConsensusProvider();

		ConsensusSettings csSettings = csProvider.getSettingsFactory().getConsensusSettingsBuilder().createSettings(csProperties, replicas);
		
	}

}
