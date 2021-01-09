package com.jd.blockchain.gateway;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.util.TypeUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.alibaba.fastjson.asm.Type;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.NodeNetworkAddresses;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.action.ActionRequest;
import com.jd.blockchain.consensus.action.ActionResponse;
import com.jd.blockchain.consensus.bftsmart.BftsmartClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueBlockSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.gateway.web.BlockBrowserController;
import com.jd.blockchain.gateway.web.GatewayLedgerLoadTimer;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.BytesValueList;
import com.jd.blockchain.ledger.ConsensusSettingsUpdateOperation;
import com.jd.blockchain.ledger.EventAccountRegisterOperation;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.ledger.LedgerTransactions;
import com.jd.blockchain.ledger.MerkleSnapshot;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.RolesConfigureOperation;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.merkletree.HashBucketEntry;
import com.jd.blockchain.ledger.merkletree.KeyIndex;
import com.jd.blockchain.ledger.merkletree.MerkleIndex;
import com.jd.blockchain.ledger.proof.MerkleKey;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.ledger.proof.MerkleTrieData;
import com.jd.blockchain.sdk.GatewayAuthRequest;
import com.jd.blockchain.utils.ArgumentSet;
import com.jd.blockchain.utils.ArgumentSet.ArgEntry;
import com.jd.blockchain.utils.BaseConstant;
import com.jd.blockchain.utils.ConsoleUtils;
import com.jd.blockchain.utils.net.NetworkAddress;
import com.jd.blockchain.utils.reflection.TypeUtils;

public class GatewayServerBooter {

	private static final String DEFAULT_GATEWAY_PROPS = "application-gw.properties";

	// 当前参与方在初始化配置中的参与方列表的编号；
	private static final String HOST_ARG = "-c";

	// sp;针对spring.config.location这个参数进行包装;
	private static final String SPRING_CF_LOCATION = BaseConstant.SPRING_CF_LOCATION;

	// 是否输出调试信息；
	private static final String DEBUG_OPT = "-debug";

	public static final String CONFIG_FOLDER_NAME = "config";

	public static final String RUNTIME_FOLDER_NAME = "runtime";

	public static final String HOME_DIR;

	public static final String RUNTIME_STORAGE_DIR;

	static {
		HOME_DIR = TypeUtils.getCodeDirOf(GatewayServerBooter.class);
		RUNTIME_STORAGE_DIR = new File(HOME_DIR, RUNTIME_FOLDER_NAME).getAbsolutePath();
		GatewayAuthRequest();
	}

	public static void main(String[] args) {
		boolean debug = false;
		try {
			ArgumentSet arguments = ArgumentSet.resolve(args,
					ArgumentSet.setting().prefix(HOST_ARG, SPRING_CF_LOCATION).option(DEBUG_OPT));
			debug = arguments.hasOption(DEBUG_OPT);
			ArgEntry argHost = arguments.getArg(HOST_ARG);
			String configFile = argHost == null ? null : argHost.getValue();
			GatewayConfigProperties configProps;
			if (configFile == null) {
				ConsoleUtils.info("Load build-in default configuration ...");
				ClassPathResource configResource = new ClassPathResource("gateway.conf");
				try (InputStream in = configResource.getInputStream()) {
					configProps = GatewayConfigProperties.resolve(in);
				}
			} else {
				ConsoleUtils.info("Load configuration ...");
				configProps = GatewayConfigProperties.resolve(argHost.getValue());
			}

			// spring config location;
			String springConfigLocation = null;
			ArgumentSet.ArgEntry spConfigLocation = arguments.getArg(SPRING_CF_LOCATION);
			if (spConfigLocation != null) {
				springConfigLocation = spConfigLocation.getValue();
			} else {
				// if no the config file, then should tip as follows. but it's not a good
				// feeling, so we create it by inputStream;
				ConsoleUtils.info(
						"no param:-sp, format: -sp /x/xx.properties, use the default application-gw.properties 	");
				ClassPathResource configResource = new ClassPathResource(DEFAULT_GATEWAY_PROPS);
				InputStream in = configResource.getInputStream();

				// 将文件写入至config目录下
				String configPath = HOME_DIR + CONFIG_FOLDER_NAME + File.separator + DEFAULT_GATEWAY_PROPS;
				File targetFile = new File(configPath);

				// 先将原来文件删除再Copy
				if (targetFile.exists()) {
					FileUtils.forceDelete(targetFile);
				}

				FileUtils.copyInputStreamToFile(in, targetFile);
				springConfigLocation = "file:" + targetFile.getAbsolutePath();
			}

			// 启动服务器；
			ConsoleUtils.info("Starting web server......");
			GatewayServerBooter booter = new GatewayServerBooter(configProps, springConfigLocation);
			booter.start();
		} catch (Exception e) {
			ConsoleUtils.error("Error!! %s", e.getMessage());
			if (debug) {
				e.printStackTrace();
			}
		}
	}

	private volatile ConfigurableApplicationContext appCtx;
	private GatewayConfigProperties config;
	private AsymmetricKeypair defaultKeyPair;
	private String springConfigLocation;

	public GatewayServerBooter(GatewayConfigProperties config, String springConfigLocation) {
		this.config = config;
		this.springConfigLocation = springConfigLocation;

		String base58Pwd = config.keys().getDefault().getPrivKeyPassword();
		if (base58Pwd == null || base58Pwd.length() == 0) {
			base58Pwd = KeyGenUtils.readPasswordString();
		}

		// 加载密钥；
		PubKey pubKey = KeyGenUtils.decodePubKey(config.keys().getDefault().getPubKeyValue());

		PrivKey privKey = null;
		String base58PrivKey = config.keys().getDefault().getPrivKeyValue();
		if (base58PrivKey == null) {
			// 注：GatewayConfigProperties 确保了 PrivKeyValue 和 PrivKeyPath 必有其一；
			privKey = KeyGenUtils.readPrivKey(config.keys().getDefault().getPrivKeyPath(), base58Pwd);
		} else {
			privKey = KeyGenUtils.decodePrivKey(base58PrivKey, base58Pwd);
		}
		defaultKeyPair = new AsymmetricKeypair(pubKey, privKey);
	}

	public synchronized void start() {
		if (this.appCtx != null) {
			throw new IllegalStateException("Gateway server is running already.");
		}
		this.appCtx = startServer(config.http().getHost(), config.http().getPort(), springConfigLocation,
				config.http().getContextPath());

		ConsoleUtils.info("\r\n\r\nStart connecting to peer ....");
		BlockBrowserController blockBrowserController = appCtx.getBean(BlockBrowserController.class);
		blockBrowserController.setDataRetrievalUrl(config.dataRetrievalUrl());
		blockBrowserController.setSchemaRetrievalUrl(config.getSchemaRetrievalUrl());
		PeerConnector peerConnector = appCtx.getBean(PeerConnector.class);

		NetworkAddress peerAddress = config.masterPeerAddress();
		peerConnector.setMasterPeer(peerAddress);
		peerConnector.connect(peerAddress, defaultKeyPair);
		// 不管连接是否成功，都需要释放许可
		GatewayLedgerLoadTimer loadTimer = appCtx.getBean(GatewayLedgerLoadTimer.class);
		loadTimer.release();
		ConsoleUtils.info("Peer[%s] is connected success!", peerAddress.toString());
	}

	public synchronized void close() {
		if (this.appCtx == null) {
			return;
		}
		this.appCtx.close();
	}

	private static ConfigurableApplicationContext startServer(String host, int port, String springConfigLocation,
			String contextPath) {
		List<String> argList = new ArrayList<String>();
		argList.add(String.format("--server.address=%s", host));
		argList.add(String.format("--server.port=%s", port));

		if (springConfigLocation != null) {
			argList.add(String.format("--spring.config.location=%s", springConfigLocation));
		}

		if (contextPath != null) {
			argList.add(String.format("--server.context-path=%s", contextPath));
		}

		String[] args = argList.toArray(new String[argList.size()]);

		// 启动服务器；
		ConfigurableApplicationContext appCtx = SpringApplication.run(GatewayConfiguration.class, args);
		return appCtx;
	}

//	private static String bootPath() {
//		try {
//			URL url = GatewayServerBooter.class.getProtectionDomain().getCodeSource().getLocation();
//			String currPath = java.net.URLDecoder.decode(url.getPath(), "UTF-8");
//			// 处理打包至SpringBoot问题
//			if (currPath.contains("!/")) {
//				currPath = currPath.substring(5, currPath.indexOf("!/"));
//			}
//			if (currPath.endsWith(".jar")) {
//				currPath = currPath.substring(0, currPath.lastIndexOf("/") + 1);
//			}
//			System.out.printf("Current Project Boot Path = %s \r\n", currPath);
//			return new File(currPath).getParent() + File.separator;
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//			throw new IllegalStateException(e.getMessage(), e);
//		}
//	}

	private static void GatewayAuthRequest() {
		DataContractRegistry.register(MerkleSnapshot.class);
		DataContractRegistry.register(MerkleTrieData.class);
		DataContractRegistry.register(MerkleLeaf.class);
		DataContractRegistry.register(MerklePath.class);
		DataContractRegistry.register(MerkleKey.class);
		DataContractRegistry.register(MerkleIndex.class);
		DataContractRegistry.register(KeyIndex.class);
		DataContractRegistry.register(HashBucketEntry.class);
		DataContractRegistry.register(BytesValue.class);
		DataContractRegistry.register(BytesValueList.class);
		DataContractRegistry.register(TransactionResponse.class);
		DataContractRegistry.register(OperationResult.class);
		DataContractRegistry.register(RolesConfigureOperation.class);
		DataContractRegistry.register(RolesConfigureOperation.RolePrivilegeEntry.class);
		DataContractRegistry.register(UserAuthorizeOperation.class);
		DataContractRegistry.register(UserAuthorizeOperation.UserRolesEntry.class);
		DataContractRegistry.register(EventAccountRegisterOperation.class);
		DataContractRegistry.register(EventPublishOperation.class);
		DataContractRegistry.register(EventPublishOperation.EventEntry.class);
		DataContractRegistry.register(ConsensusSettingsUpdateOperation.class);

		DataContractRegistry.register(GatewayAuthRequest.class);
		DataContractRegistry.register(ActionRequest.class);
		DataContractRegistry.register(ActionResponse.class);
		DataContractRegistry.register(ConsensusViewSettings.class);
		DataContractRegistry.register(NodeSettings.class);
		DataContractRegistry.register(ClientIncomingSettings.class);
		DataContractRegistry.register(BftsmartConsensusViewSettings.class);
		DataContractRegistry.register(BftsmartNodeSettings.class);
		DataContractRegistry.register(BftsmartClientIncomingSettings.class);
		DataContractRegistry.register(MsgQueueConsensusSettings.class);
		DataContractRegistry.register(MsgQueueNodeSettings.class);
		DataContractRegistry.register(MsgQueueClientIncomingSettings.class);
		DataContractRegistry.register(MsgQueueNetworkSettings.class);
		DataContractRegistry.register(MsgQueueBlockSettings.class);
		DataContractRegistry.register(NodeNetworkAddress.class);
		DataContractRegistry.register(NodeNetworkAddresses.class);
		DataContractRegistry.register(LedgerTransactions.class);
	}
}