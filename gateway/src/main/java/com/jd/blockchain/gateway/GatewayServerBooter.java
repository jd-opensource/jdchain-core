package com.jd.blockchain.gateway;

import java.io.File;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.gateway.service.LedgersManager;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopology;
import com.jd.blockchain.ledger.ConsensusTypeUpdateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.net.SSLSecurity;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

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
import com.jd.blockchain.gateway.web.DataSearchController;
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

import utils.ArgumentSet;
import utils.ArgumentSet.ArgEntry;
import utils.BaseConstant;
import utils.StringUtils;
import utils.reflection.TypeUtils;

public class GatewayServerBooter {

	private static final String DEFAULT_GATEWAY_PROPS = "application-gw.properties";

	// 当前参与方在初始化配置中的参与方列表的编号；
	private static final String HOST_ARG = "-c";

	// sp;针对spring.config.location这个参数进行包装;
	private static final String SPRING_CF_LOCATION = BaseConstant.SPRING_CF_LOCATION;

	public static final String CONFIG_FOLDER_NAME = "config";

	public static final String RUNTIME_FOLDER_NAME = "runtime";

	public static final String HOME_DIR;

	public static final String RUNTIME_STORAGE_DIR;

	// 日志配置文件
	public static final String LOG_CONFIG_FILE = "log4j.configurationFile";

	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayServerBooter.class);

	static {
		HOME_DIR = TypeUtils.getCodeDirOf(GatewayServerBooter.class);
		RUNTIME_STORAGE_DIR = new File(HOME_DIR, RUNTIME_FOLDER_NAME).getAbsolutePath();
		GatewayAuthRequest();
	}

	public static void main(String[] args) {
		try {
			ArgumentSet arguments = ArgumentSet.resolve(args, ArgumentSet.setting().prefix(HOST_ARG, SPRING_CF_LOCATION));
			ArgEntry argHost = arguments.getArg(HOST_ARG);
			String configFile = argHost == null ? null : argHost.getValue();
			GatewayConfigProperties configProps;
			if (configFile == null) {
				LOGGER.info("Load build-in default configuration ...");
				ClassPathResource configResource = new ClassPathResource("gateway.conf");
				try (InputStream in = configResource.getInputStream()) {
					configProps = GatewayConfigProperties.resolve(in);
				}
			} else {
				LOGGER.info("Load configuration ...");
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
				LOGGER.info("no param:-sp, format: -sp /x/xx.properties, use the default application-gw.properties 	");
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
			LOGGER.info("Starting web server......");
			GatewayServerBooter booter = new GatewayServerBooter(configProps, springConfigLocation);
			booter.start();
		} catch (Exception e) {
			LOGGER.error("Gateway start error", e);
		}
	}

	private volatile ConfigurableApplicationContext appCtx;
	private GatewayConfigProperties config;
	private AsymmetricKeypair defaultKeyPair;
	private String springConfigLocation;

	public GatewayServerBooter(GatewayConfigProperties config, String springConfigLocation) {
		this.config = config;
		this.springConfigLocation = springConfigLocation;

		// 加载密钥；
		PubKey pubKey;
		if(!StringUtils.isEmpty(config.keys().getDefault().getPubKeyValue())) {
			pubKey = KeyGenUtils.decodePubKey(config.keys().getDefault().getPubKeyValue());
		} else {
			X509Certificate certificate = CertificateUtils.parseCertificate(utils.io.FileUtils.readText(config.keys().getDefault().getCaPath()));
			CertificateUtils.checkValidity(certificate);
			CertificateUtils.checkCertificateRole(certificate, CertificateRole.GW);
			pubKey = CertificateUtils.resolvePubKey(certificate);
		}
		String base58Pwd = config.keys().getDefault().getPrivKeyPassword();
		PrivKey privKey;
		String privkeyString = config.keys().getDefault().getPrivKeyValue();
		if (StringUtils.isEmpty(privkeyString)) {
			privkeyString = utils.io.FileUtils.readText(config.keys().getDefault().getPrivKeyPath()).trim();
			if(privkeyString.startsWith("-----BEGIN") && privkeyString.endsWith("PRIVATE KEY-----")) {
				if(StringUtils.isEmpty(base58Pwd)) {
					privKey = CertificateUtils.parsePrivKey(pubKey.getAlgorithm(), privkeyString);
				} else {
					privKey = CertificateUtils.parsePrivKey(pubKey.getAlgorithm(), privkeyString, base58Pwd);
				}
			} else {
				if (StringUtils.isEmpty(base58Pwd)) {
					base58Pwd = KeyGenUtils.readPasswordString();
				}
				privKey = KeyGenUtils.decodePrivKey(privkeyString, base58Pwd);
			}
		} else {
			if (StringUtils.isEmpty(base58Pwd)) {
				base58Pwd = KeyGenUtils.readPasswordString();
			}
			privKey = KeyGenUtils.decodePrivKey(privkeyString, base58Pwd);
		}
		defaultKeyPair = new AsymmetricKeypair(pubKey, privKey);
	}

	public synchronized void start() {
		if (this.appCtx != null) {
			throw new IllegalStateException("Gateway server is running already.");
		}
		this.appCtx = startServer(config.http(), springConfigLocation);

		String keyStore = appCtx.getEnvironment().getProperty("server.ssl.key-store");
		String keyStoreType = appCtx.getEnvironment().getProperty("server.ssl.key-store-type");
		String keyAlias = appCtx.getEnvironment().getProperty("server.ssl.key-alias");
		String keyStorePassword = appCtx.getEnvironment().getProperty("server.ssl.key-store-password");
		String trustStore = appCtx.getEnvironment().getProperty("server.ssl.trust-store");
		String trustStorePassword = appCtx.getEnvironment().getProperty("server.ssl.trust-store-password");
		String trustStoreType = appCtx.getEnvironment().getProperty("server.ssl.trust-store-type");
		// 网关连接PEER节点管理服务TLS配置
		SSLSecurity manageSecurity = new SSLSecurity(keyStoreType, keyStore, keyAlias, keyStorePassword, trustStore, trustStorePassword, trustStoreType);
		// 网关连接PEER节点共识服务TLS配置
		SSLSecurity consensusSecurity = manageSecurity;

		LOGGER.info("Start connecting to peer ....");
		DataSearchController dataSearchController = appCtx.getBean(DataSearchController.class);
		dataSearchController.setDataRetrievalUrl(config.dataRetrievalUrl());
		dataSearchController.setSchemaRetrievalUrl(config.getSchemaRetrievalUrl());
		LedgersManager peerConnector = appCtx.getBean(LedgersManager.class);

		peerConnector.init(defaultKeyPair, manageSecurity, consensusSecurity, config);

		LOGGER.info("Peer {} is connected success!", config.masterPeerAddress().toString());
	}

	public synchronized void close() {
		if (this.appCtx == null) {
			return;
		}
		this.appCtx.close();
	}

	private static ConfigurableApplicationContext startServer(GatewayConfigProperties.HttpConfig config, String springConfigLocation) {
		List<String> argList = new ArrayList<String>();
		String logConfig = System.getProperty(LOG_CONFIG_FILE);
		if(!StringUtils.isEmpty(logConfig)) {
			argList.add(String.format("--logging.config=%s", logConfig));
		}
		argList.add(String.format("--server.address=%s", config.getHost()));
		argList.add(String.format("--server.port=%s", config.getPort()));
		argList.add(String.format("--server.ssl.enabled=%s", config.isSecure()));
		if(config.isSecure()) {
			argList.add(String.format("--server.ssl.client-auth=%s", config.getClientAuth()));
		}

		if (springConfigLocation != null) {
			argList.add(String.format("--spring.config.location=%s", springConfigLocation));
		}

		if (!StringUtils.isEmpty(config.getContextPath())) {
			argList.add(String.format("--server.context-path=%s", config.getContextPath()));
		}

		String[] args = argList.toArray(new String[argList.size()]);

		// 启动服务器；
		ConfigurableApplicationContext appCtx = SpringApplication.run(GatewayConfiguration.class, args);
		return appCtx;
	}

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
		DataContractRegistry.register(ConsensusTypeUpdateOperation.class);

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

		DataContractRegistry.register(LedgerPeersTopology.class);
	}
}