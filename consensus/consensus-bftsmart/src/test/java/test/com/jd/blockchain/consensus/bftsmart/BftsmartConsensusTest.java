package test.com.jd.blockchain.consensus.bftsmart;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.mockito.Mockito;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.utils.ConsoleUtils;
import com.jd.blockchain.utils.PropertiesUtils;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.security.RandomUtils;

public class BftsmartConsensusTest {

	private static final ConsensusProvider CS_PROVIDER = new BftsmartConsensusProvider();

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

	/**
	 * 标准功能用例：建立4个副本节点的共识网络，可以正常地达成进行共识；
	 * <p>
	 * 1. 建立 4 个副本节点的共识网络，启动全部的节点；<br>
	 * 2. 建立不少于 1 个共识客户端连接到共识网络；<br>
	 * 3. 共识客户端并发地提交共识消息，每个副本节点都能得到一致的消息队列；<br>
	 * 4. 副本节点对每一条消息都返回相同的响应，共识客户端能够得到正确的回复结果；<br>
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
//	@Test
	public void testNormal() throws IOException, InterruptedException {
		final int N = 4;
		final String realmName = Base58Utils.encode(RandomUtils.generateRandomBytes(32));

		Replica[] replicas = createReplicas(N);

		ConsensusSettings csSettings = buildConsensusSettings("classpath:bftsmart-test.config", replicas);

		StateMachineReplicate smr = Mockito.mock(StateMachineReplicate.class);
		MessageHandle messageHandler = Mockito.mock(MessageHandle.class);

		NodeServer[] nodeServers = new NodeServer[N];
		for (int i = 0; i < nodeServers.length; i++) {
			nodeServers[i] = createNodeServer(realmName, csSettings, replicas[i], messageHandler, smr);
		}

		startNodeServers(nodeServers);
		
//		Thread.sleep(10000);
		
		stopNodeServers(nodeServers);
	}

	private void startNodeServers(NodeServer[] nodeServers) throws InterruptedException {
		CountDownLatch startupLatch = new CountDownLatch(nodeServers.length);
		for (int i = 0; i < nodeServers.length; i++) {
			int id = i;
			NodeServer nodeServer = nodeServers[i];
			EXECUTOR_SERVICE.execute(new Runnable() {

				@Override
				public void run() {
					nodeServer.start();
					ConsoleUtils.info("Replica Node [%s : %s] started! ", id,
							nodeServer.getSettings().getReplicaSettings().getAddress());
					startupLatch.countDown();
				}
			});
		}

		startupLatch.await();
		ConsoleUtils.info("All replicas start success!");
	}
	
	private void stopNodeServers(NodeServer[] nodeServers) throws InterruptedException {
		CountDownLatch startupLatch = new CountDownLatch(nodeServers.length);
		for (int i = 0; i < nodeServers.length; i++) {
			int id = i;
			NodeServer nodeServer = nodeServers[i];
			EXECUTOR_SERVICE.execute(new Runnable() {
				
				@Override
				public void run() {
					nodeServer.stop();;
					ConsoleUtils.info("Replica Node [%s : %s] stop! ", id,
							nodeServer.getSettings().getReplicaSettings().getAddress());
					startupLatch.countDown();
				}
			});
		}
		
		startupLatch.await();
		ConsoleUtils.info("All replicas stop!");
	}

	private Replica[] createReplicas(int n) {
		Replica[] replicas = new Replica[n];
		for (int i = 0; i < replicas.length; i++) {
			ReplicaNode rp = new ReplicaNode(i);
			AsymmetricKeypair kp = Crypto.getSignatureFunction(ClassicAlgorithm.ED25519).generateKeypair();
			rp.setKey(kp);
			rp.setName("节点[" + i + "]");
			replicas[i] = rp;
		}

		return replicas;
	}

	private ConsensusSettings buildConsensusSettings(String configFile, Replica[] replicas) throws IOException {
		Properties csProperties = PropertiesUtils.loadProperties(configFile, "UTF-8");

		return CS_PROVIDER.getSettingsFactory().getConsensusSettingsBuilder().createSettings(csProperties, replicas);
	}

	private NodeServer createNodeServer(String realmName, ConsensusSettings csSettings, Replica replica,
			MessageHandle messageHandler, StateMachineReplicate smr) {
		ServerSettings serverSettings = CS_PROVIDER.getServerFactory().buildServerSettings(realmName, csSettings,
				replica.getAddress().toBase58());
		return CS_PROVIDER.getServerFactory().setupServer(serverSettings, messageHandler, smr);
	}

}
