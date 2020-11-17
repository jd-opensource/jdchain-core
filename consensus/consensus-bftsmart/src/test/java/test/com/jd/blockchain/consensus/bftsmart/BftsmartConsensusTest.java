package test.com.jd.blockchain.consensus.bftsmart;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.mockito.Mockito;

import com.jd.blockchain.consensus.ClientIdentification;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusSecurityException;
import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
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
import com.jd.blockchain.utils.net.NetworkAddress;
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
	/**
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ConsensusSecurityException
	 */
	@Test
	public void testNormal() throws IOException, InterruptedException, ConsensusSecurityException {
		final int N = 4;
		final String realmName = Base58Utils.encode(RandomUtils.generateRandomBytes(32));

		// 创建副本信息；
		Replica[] replicas = createReplicas(N);

		// 端口从 10000 开始，每个递增 10 ；
		NetworkAddress[] networkAddresses = createMultiPortsAddresses("127.0.0.1", N, 11600, 10);

		BftsmartConsensusSettings csSettings = buildConsensusSettings("classpath:bftsmart-test.config", replicas);
		csSettings = stubNetworkAddress(csSettings, networkAddresses);

		StateMachineReplicate smr = Mockito.mock(StateMachineReplicate.class);
		MessageHandle messageHandler = Mockito.mock(MessageHandle.class);

		NodeServer[] nodeServers = new NodeServer[N];
		for (int i = 0; i < nodeServers.length; i++) {
			nodeServers[i] = createNodeServer(realmName, csSettings, replicas[i], messageHandler, smr);
		}

		startNodeServers(nodeServers);

		final int clientCount = 4;
		AsymmetricKeypair[] clientKeys = initRandomKeys(clientCount);

		ClientIncomingSettings[] clientSettings = authClientsFrom(nodeServers, clientKeys);

		ConsensusClient[] clients = setupConsensusClients(clientSettings);
		
		ConsoleUtils.info("all clients have setup!");
		Thread.sleep(1000);
		
		for (ConsensusClient cli : clients) {
			cli.close();
		}

		stopNodeServers(nodeServers);
	}

	private ConsensusClient[] setupConsensusClients(ClientIncomingSettings[] clientIncomingSettings) {
		ConsensusClient[] clients = new ConsensusClient[clientIncomingSettings.length];

		for (int i = 0; i < clients.length; i++) {
			ClientSettings clientSettings = CS_PROVIDER.getClientFactory()
					.buildClientSettings(clientIncomingSettings[i]);
			clients[i] = CS_PROVIDER.getClientFactory().setupClient(clientSettings);
		}

		return clients;
	}

	/**
	 * 从指定节点服务器中认证客户端，返回客户端接入配置；
	 * 
	 * <p>
	 * 
	 * 对于参数中的每一个客户端密钥，从服务器列表中随机挑选一个进行认证；
	 * 
	 * <p>
	 * 
	 * 返回的客户端接入配置的数量和密钥的数量一致；
	 * 
	 * @param nodeServers
	 * @param clientKeys
	 * @return
	 * @throws ConsensusSecurityException
	 */
	private ClientIncomingSettings[] authClientsFrom(NodeServer[] nodeServers, AsymmetricKeypair[] clientKeys)
			throws ConsensusSecurityException {

		ClientIncomingSettings[] incomingSettings = new ClientIncomingSettings[clientKeys.length];

		Random rand = new Random();
		for (int i = 0; i < clientKeys.length; i++) {
			ClientIdentification clientIdentification = CS_PROVIDER.getClientFactory().buildAuthId(clientKeys[i]);

			incomingSettings[i] = nodeServers[rand.nextInt(nodeServers.length)].getConsensusManageService()
					.authClientIncoming(clientIdentification);
		}

		return incomingSettings;
	}

	private AsymmetricKeypair[] initRandomKeys(int clientCount) {
		AsymmetricKeypair[] keys = new AsymmetricKeypair[clientCount];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Crypto.getSignatureFunction(ClassicAlgorithm.ED25519).generateKeypair();
		}
		return keys;
	}

	/**
	 * 将共识设置中的节点网址替换指定的网址清单中对应的值；
	 * 
	 * <p>
	 * 
	 * 按照节点顺序与网址列表顺序一一对应。
	 * 
	 * @param csSettings
	 * @param networkAddresses
	 * @return
	 */
	private BftsmartConsensusSettings stubNetworkAddress(BftsmartConsensusSettings csSettings,
			NetworkAddress[] networkAddresses) {
		BftsmartConsensusSettings csSettingStub = Mockito.spy(csSettings);

		BftsmartNodeSettings[] nodeSettings = (BftsmartNodeSettings[]) csSettingStub.getNodes();
		nodeSettings = stubNetworkAddresses(nodeSettings, networkAddresses);

		Mockito.stub(csSettingStub.getNodes()).toReturn(nodeSettings);

		return csSettingStub;
	}

	/**
	 * 将节点配置的地址替换为网址清单中对应的值；
	 * <p>
	 * 
	 * 按照节点顺序与网址列表顺序一一对应。
	 * 
	 * @param nodeSettings
	 * @param networkAddresses
	 * @return
	 */
	private BftsmartNodeSettings[] stubNetworkAddresses(BftsmartNodeSettings[] nodeSettings,
			NetworkAddress[] networkAddresses) {
		assert nodeSettings.length == networkAddresses.length;

		BftsmartNodeSettings[] nodeSettingStubs = new BftsmartNodeSettings[nodeSettings.length];
		for (int i = 0; i < nodeSettingStubs.length; i++) {
			nodeSettingStubs[i] = Mockito.spy(nodeSettings[i]);

			Mockito.stub(nodeSettingStubs[i].getNetworkAddress()).toReturn(networkAddresses[i]);
		}
		return nodeSettingStubs;
	}

	/**
	 * 创建多端口的地址清单；
	 * 
	 * @param host      主机地址；
	 * @param n         总数量；
	 * @param portStart 起始端口号；
	 * @param portStep  每个地址的端口号递增值；
	 * @return
	 */
	private NetworkAddress[] createMultiPortsAddresses(String host, int n, int portStart, int portStep) {
		NetworkAddress[] addrs = new NetworkAddress[n];
		for (int i = 0; i < addrs.length; i++) {
			addrs[i] = new NetworkAddress(host, portStart + portStep * i);
		}
		return addrs;
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
					nodeServer.stop();
					;
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

	private BftsmartConsensusSettings buildConsensusSettings(String configFile, Replica[] replicas) throws IOException {
		Properties csProperties = PropertiesUtils.loadProperties(configFile, "UTF-8");

		return (BftsmartConsensusSettings) CS_PROVIDER.getSettingsFactory().getConsensusSettingsBuilder()
				.createSettings(csProperties, replicas);
	}

	private NodeServer createNodeServer(String realmName, ConsensusSettings csSettings, Replica replica,
			MessageHandle messageHandler, StateMachineReplicate smr) {
		ServerSettings serverSettings = CS_PROVIDER.getServerFactory().buildServerSettings(realmName, csSettings,
				replica.getAddress().toBase58());
		return CS_PROVIDER.getServerFactory().setupServer(serverSettings, messageHandler, smr);
	}

}
