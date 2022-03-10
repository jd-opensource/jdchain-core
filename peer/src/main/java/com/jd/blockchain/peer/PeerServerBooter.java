package com.jd.blockchain.peer;

import java.io.File;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.jd.blockchain.ledger.ConsensusReconfigOperation;
import com.jd.blockchain.ledger.HashAlgorithmUpdateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientCredential;
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
import com.jd.blockchain.consts.Global;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.ledger.BlockBody;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.BytesValueList;
import com.jd.blockchain.ledger.ConsensusSettingsUpdateOperation;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.DigitalSignatureBody;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventAccountRegisterOperation;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.ledger.HashObject;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.LedgerInitOperation;
import com.jd.blockchain.ledger.LedgerInitSetting;
import com.jd.blockchain.ledger.LedgerMetadata;
import com.jd.blockchain.ledger.LedgerMetadata_V2;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.LedgerTransactions;
import com.jd.blockchain.ledger.MerkleSnapshot;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.ParticipantRegisterOperation;
import com.jd.blockchain.ledger.ParticipantStateUpdateInfo;
import com.jd.blockchain.ledger.ParticipantStateUpdateOperation;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.RoleInitSettings;
import com.jd.blockchain.ledger.RoleSet;
import com.jd.blockchain.ledger.RolesConfigureOperation;
import com.jd.blockchain.ledger.SecurityInitSettings;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.UserAccountHeader;
import com.jd.blockchain.ledger.UserAuthInitSettings;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.UserInfo;
import com.jd.blockchain.ledger.UserInfoSetOperation;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.LedgerInitDecision;
import com.jd.blockchain.ledger.core.LedgerInitProposal;
import com.jd.blockchain.ledger.merkletree.HashBucketEntry;
import com.jd.blockchain.ledger.merkletree.KeyIndex;
import com.jd.blockchain.ledger.merkletree.MerkleIndex;
import com.jd.blockchain.ledger.proof.MerkleKey;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.ledger.proof.MerkleTrieData;
import com.jd.blockchain.peer.web.LedgerLoadTimer;
import com.jd.blockchain.runtime.RuntimeConstant;
import com.jd.blockchain.storage.service.DbConnectionFactory;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig;
import com.jd.blockchain.tools.initializer.web.LedgerBindingConfigException;

import utils.ArgumentSet;
import utils.BaseConstant;
import utils.StringUtils;
import utils.reflection.TypeUtils;

/**
 * 节点服务实例的启动器；
 *
 * @author huanghaiquan
 *
 */
public class PeerServerBooter {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerServerBooter.class);

	// 初始化账本绑定配置文件的路径；
	public static final String LEDGERBIND_ARG = "-c";
	// 服务地址；
	private static final String HOST_ARG = "-h";
	// 服务端口；
	private static final String PORT_ARG = "-p";

	public static final String LEDGER_BIND_CONFIG_NAME = "ledger-binding.conf";

	public static String ledgerBindConfigFile;

	// 日志配置文件
	public static final String LOG_CONFIG_FILE = "log4j.configurationFile";

	// Spring配置文件
	private static final String SPRING_CF_LOCATION = BaseConstant.SPRING_CF_LOCATION;
	public static final String HOME_DIR;

	static {
		// 加载 Global ，初始化全局设置；
		Global.initialize();

		// 注册数据契约
		registerDataContracts();

		HOME_DIR = TypeUtils.getCodeDirOf(PeerServerBooter.class);
	}


	/**
	 * 主入口方法，由启动脚本进行调用；
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		handle(args);
	}

	public static void handle(String[] args) {
		LedgerBindingConfig ledgerBindingConfig = null;
		ArgumentSet arguments = ArgumentSet.resolve(args,
				ArgumentSet.setting().prefix(LEDGERBIND_ARG, HOST_ARG, PORT_ARG, SPRING_CF_LOCATION));
		try {
			ArgumentSet.ArgEntry argLedgerBindConf = arguments.getArg(LEDGERBIND_ARG);
			ledgerBindConfigFile = argLedgerBindConf == null ? null : argLedgerBindConf.getValue();
			if (ledgerBindConfigFile == null) {
				LOGGER.info("Load build-in default configuration ...");
				ClassPathResource configResource = new ClassPathResource(LEDGER_BIND_CONFIG_NAME);
				if (configResource.exists()) {
					try (InputStream in = configResource.getInputStream()) {
						ledgerBindingConfig = LedgerBindingConfig.resolve(in);
					}
				}
			} else {
				LOGGER.info("Load configuration,ledgerBindConfigFile position=" + ledgerBindConfigFile);
				File file = new File(ledgerBindConfigFile);
				if (file.exists()) {
					try {
						ledgerBindingConfig = LedgerBindingConfig.resolve(file);
					} catch (LedgerBindingConfigException e) {
						LOGGER.info("Load ledgerBindConfigFile content is empty !!!");
					}
				}
			}
			String host = null;
			ArgumentSet.ArgEntry hostArg = arguments.getArg(HOST_ARG);
			if (hostArg != null) {
				host = hostArg.getValue();
			}
			int port = 0;
			ArgumentSet.ArgEntry portArg = arguments.getArg(PORT_ARG);
			if (portArg != null) {
				try {
					port = Integer.parseInt(portArg.getValue());
				} catch (NumberFormatException e) {
					// ignore NumberFormatException of port argument;
				}
			}
			// spring config location;
			String springConfigLocation = null;
			ArgumentSet.ArgEntry spConfigLocation = arguments.getArg(SPRING_CF_LOCATION);
			if (spConfigLocation != null) {
				springConfigLocation = spConfigLocation.getValue();
			}

			PeerServerBooter booter = new PeerServerBooter(ledgerBindingConfig, host, port, springConfigLocation);
			LOGGER.debug("PeerServerBooter's urls=" + Arrays.toString(((URLClassLoader) booter.getClass().getClassLoader()).getURLs()));
			booter.start();
		} catch (Exception e) {
			LOGGER.error("Peer start error", e);
		}
	}

	private LedgerBindingConfig ledgerBindingConfig;
	private String hostAddress;
	private int port;
	private Object[] externalBeans;
	private volatile ConfigurableApplicationContext appContext;
	private String springConfigLocation;

	public PeerServerBooter(LedgerBindingConfig ledgerBindingConfig, String hostAddress, int port,
			Object... externalBeans) {
		this(ledgerBindingConfig, hostAddress, port, null, externalBeans);
	}

	public PeerServerBooter(LedgerBindingConfig ledgerBindingConfig, String hostAddress, int port,
							String springConfigLocation, Object... externalBeans) {
		this.ledgerBindingConfig = ledgerBindingConfig;
		this.hostAddress = hostAddress;
		this.port = port;
		this.externalBeans = externalBeans;
		this.springConfigLocation = springConfigLocation;
	}

	public synchronized void start() {
		if (appContext != null) {
			throw new IllegalStateException("Peer server is running already!");
		}
		appContext = startServer(ledgerBindingConfig, hostAddress, port, springConfigLocation, externalBeans);
	}

	public synchronized void close() {
		if (appContext == null) {
			return;
		}
		ConfigurableApplicationContext ctx = appContext;
		closeServer();
		appContext = null;
		ctx.close();
	}

	public void closeServer() {
		ConsensusManage consensusManage = appContext.getBean(ConsensusManage.class);
		consensusManage.closeAllRealms();
	}

	/**
	 * 启动服务；
	 *
	 * @param ledgerBindingConfig 账本绑定配置；
	 * @param hostAddress         服务地址；如果为空，则采用默认配置;
	 * @param port                端口地址；如果小于等于 0 ，则采用默认配置；
	 * @return
	 */
	private static ConfigurableApplicationContext startServer(LedgerBindingConfig ledgerBindingConfig,
			String hostAddress, int port, String springConfigLocation, Object... externalBeans) {
		List<String> argList = new ArrayList<String>();
		String logConfig = System.getProperty(LOG_CONFIG_FILE);
		if(!StringUtils.isEmpty(logConfig)) {
			argList.add(String.format("--logging.config=%s", logConfig));
		}
		String argServerAddress = String.format("--server.address=%s", "0.0.0.0");
		argList.add(argServerAddress);
		if (port > 0) {
			String argServerPort = String.format("--server.port=%s", port);
			argList.add(argServerPort);
		} else {
			port = 8080;
		}
		if (springConfigLocation != null) {
			argList.add(String.format("--spring.config.location=%s", springConfigLocation));
		}
		String[] args = argList.toArray(new String[argList.size()]);

		SpringApplication app = new SpringApplication(PeerConfiguration.class);
		if (externalBeans != null && externalBeans.length > 0) {
			app.addInitializers((ApplicationContextInitializer<ConfigurableApplicationContext>) applicationContext -> {
				ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
				for (Object bean : externalBeans) {
					if (bean != null) {
						beanFactory.registerSingleton(bean.toString(), bean);
					}
				}
			});
		}
		// 启动 web 服务；
		ConfigurableApplicationContext ctx = app.run(args);
		RuntimeConstant.setMonitorProperties(port, Boolean.valueOf(ctx.getEnvironment().getProperty("server.ssl.enabled")));
		// 配置文件为空，则说明目前没有账本，不需要配置账本相关信息
		if (ledgerBindingConfig != null) {
			// 建立共识网络；
			Map<String, LedgerBindingConfigAware> bindingConfigAwares = ctx
					.getBeansOfType(LedgerBindingConfigAware.class);
			for (LedgerBindingConfigAware aware : bindingConfigAwares.values()) {
				aware.setConfig(ledgerBindingConfig);
			}
			ConsensusManage consensusManage = ctx.getBean(ConsensusManage.class);
			consensusManage.runAllRealms();
		}
		// 释放定时任务许可
		LedgerLoadTimer loadTimerBean = ctx.getBean(LedgerLoadTimer.class);
		loadTimerBean.release();

		return ctx;
	}

	public DbConnectionFactory getDBConnectionFactory() {
		return appContext.getBean(DbConnectionFactory.class);
	}

	private static void registerDataContracts() {
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
		DataContractRegistry.register(BlockchainIdentity.class);
		DataContractRegistry.register(LedgerBlock.class);
		DataContractRegistry.register(BlockBody.class);
		DataContractRegistry.register(LedgerDataSnapshot.class);
		DataContractRegistry.register(LedgerAdminInfo.class);
		DataContractRegistry.register(TransactionContent.class);
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(TransactionResult.class);
		DataContractRegistry.register(LedgerTransaction.class);
		DataContractRegistry.register(Operation.class);
		DataContractRegistry.register(LedgerInitOperation.class);
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(UserInfoSetOperation.class);
		DataContractRegistry.register(UserInfoSetOperation.KVEntry.class);
		DataContractRegistry.register(DataAccountRegisterOperation.class);
		DataContractRegistry.register(DataAccountKVSetOperation.class);
		DataContractRegistry.register(DataAccountKVSetOperation.KVWriteEntry.class);
		DataContractRegistry.register(ContractCodeDeployOperation.class);
		DataContractRegistry.register(ContractEventSendOperation.class);
		DataContractRegistry.register(ParticipantRegisterOperation.class);
		DataContractRegistry.register(ParticipantStateUpdateOperation.class);
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
		DataContractRegistry.register(ConsensusReconfigOperation.class);
		DataContractRegistry.register(HashAlgorithmUpdateOperation.class);
		DataContractRegistry.register(PrivilegeSet.class);
		DataContractRegistry.register(RoleSet.class);
		DataContractRegistry.register(SecurityInitSettings.class);
		DataContractRegistry.register(RoleInitSettings.class);
		DataContractRegistry.register(UserAuthInitSettings.class);
		DataContractRegistry.register(Event.class);
		DataContractRegistry.register(LedgerMetadata.class);
		DataContractRegistry.register(LedgerMetadata_V2.class);
		DataContractRegistry.register(LedgerInitSetting.class);
		DataContractRegistry.register(LedgerInitProposal.class);
		DataContractRegistry.register(LedgerInitDecision.class);
		DataContractRegistry.register(LedgerSettings.class);
		DataContractRegistry.register(ParticipantNode.class);
		DataContractRegistry.register(ParticipantStateUpdateInfo.class);
		DataContractRegistry.register(CryptoSetting.class);
		DataContractRegistry.register(CryptoProvider.class);
		DataContractRegistry.register(DataAccountInfo.class);
		DataContractRegistry.register(UserAccountHeader.class);
		DataContractRegistry.register(UserInfo.class);
		DataContractRegistry.register(ContractInfo.class);
		DataContractRegistry.register(HashObject.class);
		DataContractRegistry.register(CryptoAlgorithm.class);
		DataContractRegistry.register(DigitalSignature.class);
		DataContractRegistry.register(DigitalSignatureBody.class);
		DataContractRegistry.register(ClientCredential.class);
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
