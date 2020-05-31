package com.jd.blockchain.peer.web;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import bftsmart.reconfiguration.Reconfiguration;
import bftsmart.reconfiguration.ReconfigureReply;
import bftsmart.reconfiguration.util.HostsConfig;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.reconfiguration.views.MemoryBasedViewStorage;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.ServiceProxy;
import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.*;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.service.TransactionBatchResultHandle;
import com.jd.blockchain.tools.initializer.DBConnectionConfig;
import com.jd.blockchain.transaction.SignatureUtils;
import com.jd.blockchain.transaction.TxRequestMessage;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.blockchain.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientIdentification;
import com.jd.blockchain.consensus.ClientIdentifications;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.action.ActionResponse;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.mq.server.MsgQueueMessageDispatcher;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.ledger.core.LedgerManage.BlockGeneratedListener;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.ledger.proof.MerkleData;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.peer.ConsensusRealm;
import com.jd.blockchain.peer.LedgerBindingConfigAware;
import com.jd.blockchain.peer.PeerManage;
import com.jd.blockchain.peer.consensus.LedgerStateManager;
import com.jd.blockchain.setting.GatewayIncomingSetting;
import com.jd.blockchain.setting.LedgerIncomingSetting;
import com.jd.blockchain.storage.service.DbConnection;
import com.jd.blockchain.storage.service.DbConnectionFactory;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig.BindingConfig;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.ConsoleUtils;
import com.jd.blockchain.utils.io.ByteArray;
import com.jd.blockchain.utils.net.NetworkAddress;
import com.jd.blockchain.web.converters.BinaryMessageConverter;

import static com.jd.blockchain.ledger.TransactionState.LEDGER_ERROR;

/**
 * 网关管理服务；
 * 
 * 提供
 * 
 * @author huanghaiquan
 *
 */
@RestController
@RequestMapping(path = "/management")
public class ManagementController implements LedgerBindingConfigAware, PeerManage {

	private static Logger LOGGER = LoggerFactory.getLogger(ManagementController.class);

	public static final String GATEWAY_PUB_EXT_NAME = ".gw.pub";

	public static final int MIN_GATEWAY_ID = 10000;

	@Autowired
	private LedgerManage ledgerManager;

	@Autowired
	private DbConnectionFactory connFactory;

	private Map<HashDigest, MsgQueueMessageDispatcher> ledgerTxConverters = new ConcurrentHashMap<>();

	private Map<HashDigest, NodeServer> ledgerPeers = new ConcurrentHashMap<>();

	private Map<HashDigest, CryptoSetting> ledgerCryptoSettings = new ConcurrentHashMap<>();

	private Map<HashDigest, DBConnectionConfig> ledgerDBConnects = new ConcurrentHashMap<>();

	private Map<HashDigest, AsymmetricKeypair> ledgerKeypairs = new ConcurrentHashMap<>();

	private Map<HashDigest, ParticipantNode> ledgerCurrNodes = new ConcurrentHashMap<>();

	private LedgerBindingConfig config;

	private ServiceProxy peerProxy;

	@Autowired
	private MessageHandle consensusMessageHandler;

	@Autowired
	private StateMachineReplicate consensusStateManager;

	static {
		DataContractRegistry.register(LedgerInitOperation.class);
		DataContractRegistry.register(LedgerBlock.class);
		DataContractRegistry.register(TransactionContent.class);
		DataContractRegistry.register(TransactionContentBody.class);
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(NodeRequest.class);
		DataContractRegistry.register(EndpointRequest.class);
		DataContractRegistry.register(TransactionResponse.class);
		DataContractRegistry.register(DataAccountKVSetOperation.class);
		DataContractRegistry.register(DataAccountKVSetOperation.KVWriteEntry.class);

		DataContractRegistry.register(Operation.class);
		DataContractRegistry.register(ContractCodeDeployOperation.class);
		DataContractRegistry.register(ContractEventSendOperation.class);
		DataContractRegistry.register(DataAccountRegisterOperation.class);
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(ParticipantRegisterOperation.class);
		DataContractRegistry.register(ParticipantStateUpdateOperation.class);

		DataContractRegistry.register(ActionResponse.class);

		DataContractRegistry.register(BftsmartConsensusSettings.class);
		DataContractRegistry.register(BftsmartNodeSettings.class);

		DataContractRegistry.register(LedgerAdminInfo.class);
		DataContractRegistry.register(LedgerSettings.class);

		// 注册角色/权限相关接口
		DataContractRegistry.register(RolesConfigureOperation.class);
		DataContractRegistry.register(RolesConfigureOperation.RolePrivilegeEntry.class);
		DataContractRegistry.register(UserAuthorizeOperation.class);
		DataContractRegistry.register(UserAuthorizeOperation.UserRolesEntry.class);
		DataContractRegistry.register(PrivilegeSet.class);
		DataContractRegistry.register(RoleSet.class);
		DataContractRegistry.register(SecurityInitSettings.class);
		DataContractRegistry.register(RoleInitSettings.class);
		DataContractRegistry.register(UserAuthInitSettings.class);
		DataContractRegistry.register(LedgerMetadata_V2.class);

		// 注册默克尔树相关接口
		DataContractRegistry.register(MerkleData.class);
		DataContractRegistry.register(MerkleLeaf.class);
		DataContractRegistry.register(MerklePath.class);

		// 注册加解密相关接口
		DataContractRegistry.register(CryptoSetting.class);
		DataContractRegistry.register(CryptoProvider.class);
		DataContractRegistry.register(CryptoAlgorithm.class);
		// TransactionSetQuery;
		DataContractRegistry.register(TransactionSetQuery.class);
	}

	/**
	 * 接入认证；
	 * 
	 * @param clientIdentifications
	 * @return
	 */
	@RequestMapping(path = "/gateway/auth", method = RequestMethod.POST, consumes = BinaryMessageConverter.CONTENT_TYPE_VALUE)
	public GatewayIncomingSetting authenticateGateway(@RequestBody ClientIdentifications clientIdentifications) {
		// 去掉不严谨的网关注册和认证逻辑；暂时先放开，不做认证，后续应该在链上注册网关信息，并基于链上的网关信息进行认证；
		// by: huanghaiquan; at 2018-09-11 18:34;
		// TODO: 实现网关的链上注册与认证机制；
		// TODO: 暂时先返回全部账本对应的共识网络配置信息；以账本哈希为 key 标识每一个账本对应的共识域、以及共识配置参数；
		if (ledgerPeers.size() == 0 || clientIdentifications == null) {
			return null;
		}

		ClientIdentification[] identificationArray = clientIdentifications.getClientIdentifications();
		if (identificationArray == null || identificationArray.length <= 0) {
			return null;
		}

		GatewayIncomingSetting setting = new GatewayIncomingSetting();
		List<LedgerIncomingSetting> ledgerIncomingList = new ArrayList<LedgerIncomingSetting>();

		for (HashDigest ledgerHash : ledgerPeers.keySet()) {

			NodeServer peer = ledgerPeers.get(ledgerHash);

			String peerProviderName = peer.getProviderName();

			ConsensusProvider provider = ConsensusProviders.getProvider(peer.getProviderName());

			ClientIncomingSettings clientIncomingSettings = null;
			for (ClientIdentification authId : identificationArray) {
				if (authId.getProviderName() == null || authId.getProviderName().length() <= 0
						|| !authId.getProviderName().equalsIgnoreCase(peerProviderName)) {
					continue;
				}
				try {
					clientIncomingSettings = peer.getConsensusManageService().authClientIncoming(authId);

					// add for test the gateway connect to peer0; 20200514;
					ConsensusSettings consensusSettings = clientIncomingSettings.getConsensusSettings();
					if (consensusSettings instanceof BftsmartConsensusSettings) {
						BftsmartConsensusSettings settings = (BftsmartConsensusSettings) consensusSettings;
						NodeSettings[] nodeSettings = settings.getNodes();
						if (nodeSettings != null) {
							for (NodeSettings ns : nodeSettings) {
								if (ns instanceof BftsmartNodeSettings) {
									BftsmartNodeSettings bftNs = (BftsmartNodeSettings) ns;
									NetworkAddress address = bftNs.getNetworkAddress();
									ConsoleUtils.info("PartiNode id = %s, host = %s, port = %s \r\n", bftNs.getId(),
											address.getHost(), address.getPort());
								}
							}
						}
					}

					break;
				} catch (Exception e) {
					throw new AuthenticationServiceException(e.getMessage(), e);
				}
			}
			if (clientIncomingSettings == null) {
				continue;
			}

			byte[] clientIncomingBytes = provider.getSettingsFactory().getIncomingSettingsEncoder()
					.encode(clientIncomingSettings);
			String base64ClientIncomingSettings = ByteArray.toBase64(clientIncomingBytes);

			LedgerIncomingSetting ledgerIncomingSetting = new LedgerIncomingSetting();
			ledgerIncomingSetting.setLedgerHash(ledgerHash);

			// 使用非代理对象，防止JSON序列化异常
			ledgerIncomingSetting.setCryptoSetting(new CryptoConfigInfo(ledgerCryptoSettings.get(ledgerHash)));
			ledgerIncomingSetting.setClientSetting(base64ClientIncomingSettings);
			ledgerIncomingSetting.setProviderName(peerProviderName);

			ledgerIncomingList.add(ledgerIncomingSetting);

		}
		setting.setLedgers(ledgerIncomingList.toArray(new LedgerIncomingSetting[ledgerIncomingList.size()]));
		return setting;
	}

	@Override
	public void setConfig(LedgerBindingConfig config) {
		// TODO 更新配置；暂时不考虑变化过程的平滑切换问题,后续完善该流程；
		// 1、检查账本的数据库配置；a、配置发生变化的账本，建立新的账本库(LedgerRepository)替换旧的实例；b、加入新增加的账本库实例；c、移除已经废弃的账本库；
		// 2、完成账本库更改后，读取最新的共识配置信息，更新共识域；
		// 3、基于当前共识地址检查共识域；a、启动新增加的共识地址，以及更新相应的共识域关系；c、已经废弃的共识域直接停止；
		try {
			// remove all existing ledger repositories;
			HashDigest[] existingLedgerHashs = ledgerManager.getLedgerHashs();
			for (HashDigest lh : existingLedgerHashs) {
				ledgerManager.unregister(lh);
			}
			HashDigest[] ledgerHashs = config.getLedgerHashs();
			for (HashDigest ledgerHash : ledgerHashs) {
				setConfig(config.getLedger(ledgerHash), ledgerHash);
			}

			this.config = config;

		} catch (Exception e) {
			LOGGER.error("Error occurred on configing LedgerBindingConfig! --" + e.getMessage(), e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	public NodeServer setConfig(BindingConfig bindingConfig, HashDigest ledgerHash) {
//		LedgerBindingConfig.BindingConfig bindingConfig = config.getLedger(ledgerHash);
		DbConnection dbConnNew = connFactory.connect(bindingConfig.getDbConnection().getUri(),
				bindingConfig.getDbConnection().getPassword());
		LedgerQuery ledgerRepository = ledgerManager.register(ledgerHash, dbConnNew.getStorageService());

		LedgerAdminInfo ledgerAdminAccount = ledgerRepository.getAdminInfo();

		// load provider;
		ConsensusProvider provider = getProvider(ledgerAdminAccount);

		// load consensus setting;
		ConsensusSettings csSettings = getConsensusSetting(provider, ledgerAdminAccount);

		// find current node;
		ParticipantNode currentNode = null;
		for (ParticipantNode participantNode : ledgerAdminAccount.getParticipants()) {
			if (participantNode.getAddress().toString().equals(bindingConfig.getParticipant().getAddress())) {
				currentNode = participantNode;
			}
		}
		if (currentNode == null) {
			throw new IllegalArgumentException(
					"Current node is not found from the participant settings of ledger[" + ledgerHash.toBase58() + "]!");
		}

		ledgerCurrNodes.put(ledgerHash, currentNode);

		// 添加一个账本上新区块生成时的监听者
		addListener((LedgerRepository) ledgerRepository);

		NodeServer server = null;

		// 处于ACTIVED状态的参与方才会创建共识节点
		if (currentNode.getParticipantNodeState() == ParticipantNodeState.ACTIVED) {

			ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(ledgerHash.toBase58(),
					csSettings, currentNode.getAddress().toString());

			((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());
			((LedgerStateManager) consensusStateManager).setLatestViewId(0);

			server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
					consensusStateManager);
			ledgerPeers.put(ledgerHash, server);
		}

		ledgerCryptoSettings.put(ledgerHash, ledgerAdminAccount.getSettings().getCryptoSetting());
		ledgerDBConnects.put(ledgerHash, bindingConfig.getDbConnection());
		ledgerKeypairs.put(ledgerHash, loadIdentity(currentNode, bindingConfig));

		return server;
	}

	@Override
	public ConsensusRealm[] getRealms() {
		throw new IllegalStateException("Not implemented!");
	}

	@Override
	public void runAllRealms() {
		for (NodeServer peer : ledgerPeers.values()) {
			runRealm(peer);
		}
	}

	@Override
	public void runRealm(NodeServer nodeServer) {
		nodeServer.start();
	}

	@Override
	public void closeAllRealms() {
		for (NodeServer peer : ledgerPeers.values()) {
			peer.stop();
		}
	}

	/**
	 * 代理交易； <br>
	 * 
	 * 此方法假设当前节点是一个新建但尚未加入共识网络的共识节点, 通过此方法接收一笔用于实现管理操作的交易；
	 * 
	 * <p>
	 * 
	 * 此方法接收到交易之后，先把交易提交到已有的共识网络执行； <br>
	 * 
	 * 如果交易通过验证并执行成功，则将交易在本地的账本中以本地方式执行; <br>
	 * 
	 * 如果执行之后的新区块一致，则提交本地区块；
	 * 
	 * <p>
	 * 如果操作中涉及到共识参与方的共识参数变化，将触发将此节点的共识拓扑改变的操作；
	 * 
	 * @param txRequest
	 * @return
	 */
	@RequestMapping(path = "/delegate/tx", method = RequestMethod.POST, consumes = BinaryMessageConverter.CONTENT_TYPE_VALUE)
	public TransactionResponse process(@RequestBody TransactionRequest txRequest) {

		ConsensusSettings origConsensusSetting;
		HashDigest remoteNewBlockHash;
		DBConnectionConfig dbConnectionConfig;
		TransactionResponse transactionResponse = new TxResponseMessage();

		// 获得交易要写入的本地账本
		HashDigest ledgerHash = txRequest.getTransactionContent().getLedgerHash();

		dbConnectionConfig = ledgerDBConnects.get(ledgerHash);

		DbConnection dbConnNew = connFactory.connect(dbConnectionConfig.getUri(), dbConnectionConfig.getPassword());

		LedgerRepository ledgerRepo = (LedgerRepository) ledgerManager.register(ledgerHash, dbConnNew.getStorageService());

		// 验证本参与方是否已经被注册，没有被注册的参与方不能进行状态更新
		if (!verifyState(ledgerRepo)) {
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.PARTICIPANT_DOES_NOT_EXIST);
			return txResponseWrapper(transactionResponse);
		}

		// 验证交易的合法性
		if (!verifyTx(txRequest)) {
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.REJECTED_BY_SECURITY_POLICY);
			return txResponseWrapper(transactionResponse);
		}

		// 为交易添加本节点的签名信息，防止无法通过安全策略检查
		txRequest = addNodeSigner(txRequest);

		// 从本地加载的账本中检索出共识网络中其它参与共识的节点的网络地址；
		origConsensusSetting = SearchOrigNodes(ledgerRepo.getAdminInfo());

		// 连接已有的共识网络,把交易提交到目标账本的原有共识网络进行共识；
		TransactionResponse txResponse = commitTxToOrigConsensus(txRequest, origConsensusSetting);
		
		// 如果交易执行失败，则返回失败结果；
		if (!txResponse.isSuccess()) {
			LOGGER.error("[ManagementController] : Commit tx to orig consensus, tx execute failed!");
			return txResponse;
		}
		
		// 如果交易执行成功，记录远程共识网络的新区块哈希；
		remoteNewBlockHash = txResponse.getBlockHash();

		// 在本地账本执行交易；
		// 验证本地区块与远程区块是否一致，如果不一致，返回失败结果；
		// 如果区块一致，提交区块；
		txResponse = commitTxToLocalLedger(ledgerRepo, remoteNewBlockHash, txRequest);

		return txResponse;
		
	}

	private boolean verifyState(LedgerRepository ledgerRepo) {
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepo.getHash());

		for (ParticipantNode participantNode : ledgerRepo.getAdminInfo().getParticipants()) {
			if ((participantNode.getAddress().toString().equals(currNode.getAddress().toString())) && participantNode.getParticipantNodeState() == ParticipantNodeState.REGISTERED) {
				return true;
			}
		}
		// 新参与方不存在或者存在但已经被更新
		LOGGER.info("Participant state error, the transaction request cannot be executed!");
		return false;
	}

	// 加载本参与方的公私钥对身份信息
	private AsymmetricKeypair loadIdentity(ParticipantNode currentNode, BindingConfig bindingConfig) {

		PubKey pubKey = currentNode.getPubKey();

		String privKeyString = bindingConfig.getParticipant().getPk();

		String pwd = bindingConfig.getParticipant().getPassword();

		PrivKey privKey = KeyGenUtils.decodePrivKey(privKeyString, pwd);

		return new AsymmetricKeypair(pubKey, privKey);

	}

	// 进行区块产生事件的监听
	private void addListener(LedgerRepository ledgerRepository) {

		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());

		ledgerManager.addListener(ledgerRepository.getHash(), new BlockGeneratedListener() {
			@Override
			public void onBlockGenerated(LedgerBlock newBlock) {
				ParticipantNodeState currNodeLastState = null;
				ParticipantNodeState currNodeNewState = null;

				ParticipantNode[] lastBblockParticipants = ledgerRepository.getAdminInfo(ledgerRepository.getBlock(newBlock.getHeight() - 1)).getParticipants();

				// 检查本参与方以前的共识状态
				for(ParticipantNode participantNode : lastBblockParticipants) {
					if (participantNode.getAddress().toString().equals(currNode.getAddress().toString())) {
						currNodeLastState = participantNode.getParticipantNodeState();
						break;
					}
				}

				ParticipantNode[] newBblockParticipants = ledgerRepository.getAdminInfo(newBlock).getParticipants();

				// 检查本参与方当前的共识状态
				for(ParticipantNode participantNode : newBblockParticipants) {
					if (participantNode.getAddress().toString().equals(currNode.getAddress().toString())) {
						currNodeNewState = participantNode.getParticipantNodeState();
						break;
					}
				}

				if (currNodeLastState != null && currNodeNewState != null ) {
					// 如果参与方的状态由 false 变为 true ，则创建对应的共识节点，更新共识视图加入共识网络；
					if (currNodeLastState.CODE == ParticipantNodeState.REGISTERED.CODE && currNodeNewState.CODE == ParticipantNodeState.ACTIVED.CODE) {
						View newView = updateView(ledgerRepository);
						// 启动共识节点
						if (newView != null) {
							setupServer(ledgerRepository, newView);
						}
					} else if (currNodeLastState.CODE == ParticipantNodeState.ACTIVED.CODE && currNodeNewState.CODE == ParticipantNodeState.REGISTERED.CODE) {
						// 如果参与方的状态由 true 变为 false，则停止节点，更新共识视图从共识网络移除节点；
					} else {
						// 不做任何操作；
					}
				}
			}
		});

	}

	// 视图更新完成，启动共识节点
	private void setupServer(LedgerRepository ledgerRepository, View newView) {
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());

		LedgerAdminInfo ledgerAdminAccount = ledgerRepository.getAdminInfo(ledgerRepository.getBlock(ledgerRepository.retrieveLatestBlockHeight()));

		// load provider;
		ConsensusProvider provider = getProvider(ledgerAdminAccount);

		// load consensus setting;
		ConsensusSettings csSettings = getConsensusSetting(provider, ledgerAdminAccount);

		ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(ledgerRepository.getHash().toBase58(),
				csSettings, currNode.getAddress().toString());

		((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());
		((LedgerStateManager) consensusStateManager).setLatestViewId(newView.getId());

		NodeServer server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
				consensusStateManager);

		ledgerPeers.put(ledgerRepository.getHash(), server);

		runRealm(server);

	}

	// 通知原有的共识网络更新共识的视图ID
	private View updateView(LedgerRepository ledgerRepository) {
		NetworkAddress newPeer = null;
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());

		LOGGER.info("ManagementController start updateView operation!");

		try {

			BftsmartConsensusSettings currConsensusSettings = (BftsmartConsensusSettings) SearchOrigNodes(ledgerRepository.getAdminInfo(ledgerRepository.getBlock(ledgerRepository.retrieveLatestBlockHeight())));

			// 找到当前参与方对应的共识网络配置
			for (NodeSettings nodeSettings : currConsensusSettings.getNodes()) {
				if (nodeSettings.getAddress().equals(currNode.getAddress().toString())) {
					newPeer = ((BftsmartNodeSettings)nodeSettings).getNetworkAddress();
					break;
				}
			}

			// 找到旧的共识环境中节点的网络配置信息，并以此为依据建立共识的代理客户端
			BftsmartConsensusSettings origConsensusSettings = (BftsmartConsensusSettings) SearchOrigNodes(ledgerRepository.getAdminInfo(ledgerRepository.getBlock(ledgerRepository.retrieveLatestBlockHeight() - 1)));

			ServiceProxy peerProxy = createPeerProxy(origConsensusSettings);

			Reconfiguration reconfiguration = new Reconfiguration(peerProxy.getProcessId(), peerProxy);

			// addServer的第一个参数指待加入共识的新参与方的编号
			reconfiguration.addServer(peerProxy.getViewManager().getCurrentViewN(), newPeer.getHost(), newPeer.getPort());

			// 执行更新目标共识网络的视图ID
			ReconfigureReply reconfigureReply = reconfiguration.execute();

			// 返回新视图
			return reconfigureReply.getView();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private TransactionRequest addNodeSigner(TransactionRequest txRequest) {
		TxRequestMessage txMessage = new TxRequestMessage(txRequest);

		HashDigest ledgerHash = txRequest.getTransactionContent().getLedgerHash();
		AsymmetricKeypair peerKeypair = ledgerKeypairs.get(ledgerHash);
		CryptoSetting cryptoSetting = ledgerCryptoSettings.get(ledgerHash);
		DigitalSignature nodeSigner = SignatureUtils.sign(txRequest.getTransactionContent(), peerKeypair);

		txMessage.addNodeSignatures(nodeSigner);

		// 计算交易哈希；
		byte[] nodeRequestBytes = BinaryProtocol.encode(txMessage, TransactionRequest.class);
		HashFunction hashFunc = Crypto.getHashFunction(cryptoSetting.getHashAlgorithm());
		HashDigest txHash = hashFunc.hash(nodeRequestBytes);
		txMessage.setHash(txHash);

		return txMessage;
	}

	private TransactionResponse txResponseWrapper(TransactionResponse txResponse) {
		return new TxResponseMessage(txResponse, null);
	}

	private TransactionResponse commitTxToLocalLedger(LedgerRepository ledgerRepository, HashDigest remoteNewBlockHash, TransactionRequest txRequest) {

		TransactionBatchResultHandle handle = null;

		TransactionResponse transactionResponse = new TxResponseMessage();

		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();

		TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(ledgerRepository, opReg);

		try {
			txbatchProcessor.schedule(txRequest);

			handle = txbatchProcessor.prepare();

			// 验证本地区块与远程区块是否一致，如果不一致，返回失败结果；
			// 如果区块一致，提交区块；
			if (handle.getBlock().getHash().equals(remoteNewBlockHash)) {
				handle.commit();
			} else {
				throw new IllegalStateException("New block hash is inconsistent, will rollback!");
			}
		} catch (Exception e) {
			handle.cancel(LEDGER_ERROR);
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.LEDGER_ERROR);
			e.printStackTrace();
			return transactionResponse;
		}

		return txResponseWrapper(handle.getResponses().next());
	}

	private ServiceProxy createPeerProxy(ConsensusSettings consensusSettings) {

		HostsConfig hostsConfig;
		Properties systemConfig;
		List<HostsConfig.Config> configList = new ArrayList<>();
		List<InetSocketAddress> nodeAddresses = new ArrayList<>();

		if (peerProxy != null) {
			return peerProxy;
		}

		for (NodeSettings nodeSettings : consensusSettings.getNodes()) {
			BftsmartNodeSettings node = (BftsmartNodeSettings)nodeSettings;
			configList.add(new HostsConfig.Config(node.getId(), node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort()));
			nodeAddresses.add(new InetSocketAddress(node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort()));
		}

		// 构建共识的代理客户端需要的主机配置和系统参数配置结构
		hostsConfig = new HostsConfig(configList.toArray(new HostsConfig.Config[configList.size()]));

		systemConfig = PropertiesUtils.createProperties(((BftsmartConsensusSettings)consensusSettings).getSystemConfigs());

		int clientId = Integer.parseInt(systemConfig.getProperty("system.ttp.id"));

		// 构建tom 配置
		TOMConfiguration tomConfig = new TOMConfiguration(consensusSettings.getNodes().length, systemConfig, hostsConfig);

		tomConfig.setProcessId(clientId);

		// 根据配置信息构建客户端视图，VIEW ID没有持久化，所以不能确定原有共识网络中的视图信息，在这里先使用默认的0
		View view = new View(0, tomConfig.getInitialView(), tomConfig.getF(), nodeAddresses.toArray(new InetSocketAddress[nodeAddresses.size()]));

		MemoryBasedViewStorage viewStorage = new MemoryBasedViewStorage(view);

		// 构建共识的代理客户端，连接目标共识节点，并递交交易进行共识过程
		peerProxy = new ServiceProxy(tomConfig, viewStorage, null, null);

		return peerProxy;

	}

	// SDK 通过Peer节点转发交易到远端的共识网络
	private TransactionResponse commitTxToOrigConsensus(TransactionRequest txRequest, ConsensusSettings origConsensusSetting) {
		TransactionResponse transactionResponse = new TxResponseMessage();
		ServiceProxy peerProxy = createPeerProxy(origConsensusSetting);
		byte[] result = peerProxy.invokeOrdered(BinaryProtocol.encode(txRequest, TransactionRequest.class));

		if (result == null) {
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.CONSENSUS_ERROR);
			return transactionResponse;
		}

		return txResponseWrapper(BinaryProtocol.decode(result));
	}


	private ConsensusProvider getProvider(LedgerAdminInfo ledgerAdminInfo) {
		// load provider;
		String consensusProvider = ledgerAdminInfo.getSettings().getConsensusProvider();
		ConsensusProvider provider = ConsensusProviders.getProvider(consensusProvider);

		return provider;

	}

	private ConsensusSettings getConsensusSetting(ConsensusProvider provider, LedgerAdminInfo ledgerAdminInfo) {

		// load consensus setting
		Bytes csSettingBytes = ledgerAdminInfo.getSettings().getConsensusSetting();
		ConsensusSettings csSettings = provider.getSettingsFactory().getConsensusSettingsEncoder()
				.decode(csSettingBytes.toBytes());

		return csSettings;
	}

	private ConsensusSettings SearchOrigNodes(LedgerAdminInfo ledgerAdminInfo) {

		// 暂时不考虑排除本节点以及非激活状态共识节点的情况，因为本地账本还没有更新，链上存储的所有节点原则上都是原有共识网络中的节点
		// load provider;
		ConsensusProvider provider = getProvider(ledgerAdminInfo);

		// load consensus setting
		ConsensusSettings csSettings = getConsensusSetting(provider, ledgerAdminInfo);

		return csSettings;
	}

	private boolean verifyTx(TransactionRequest txRequest) {
		TransactionRequestExtension reqExt = new TransactionRequestExtensionImpl(txRequest);
		try {
			checkEndpointSignatures(reqExt);
			checkNodeSignatures(reqExt);
		} catch (IllegalTransactionException e) {
			return false;
		}
		return true;
	}

	private void checkEndpointSignatures(TransactionRequestExtension request) {
		TransactionContent txContent = request.getTransactionContent();
		Collection<TransactionRequestExtension.Credential> endpoints = request.getEndpoints();
		if (endpoints != null) {
			for (TransactionRequestExtension.Credential endpoint : endpoints) {
				if (!SignatureUtils.verifyHashSignature(txContent.getHash(), endpoint.getSignature().getDigest(),
						endpoint.getPubKey())) {
					// 由于签名校验失败，引发IllegalTransactionException，使外部调用抛弃此交易请求；
					throw new IllegalTransactionException(
							String.format("Wrong transaction endpoint signature! --[Tx Hash=%s][Endpoint Signer=%s]!",
									request.getTransactionContent().getHash(), endpoint.getAddress()),
							TransactionState.IGNORED_BY_WRONG_CONTENT_SIGNATURE);
				}
			}
		}
	}

	private void checkNodeSignatures(TransactionRequestExtension request) {
		TransactionContent txContent = request.getTransactionContent();
		Collection<TransactionRequestExtension.Credential> nodes = request.getNodes();
		if (nodes != null) {
			for (TransactionRequestExtension.Credential node : nodes) {
				if (!SignatureUtils.verifyHashSignature(txContent.getHash(), node.getSignature().getDigest(),
						node.getPubKey())) {
					// 由于签名校验失败，引发IllegalTransactionException，使外部调用抛弃此交易请求；
					throw new IllegalTransactionException(
							String.format("Wrong transaction node signature! --[Tx Hash=%s][Node Signer=%s]!",
									request.getTransactionContent().getHash(), node.getAddress()),
							TransactionState.IGNORED_BY_WRONG_CONTENT_SIGNATURE);
				}
			}
		}
	}
}
