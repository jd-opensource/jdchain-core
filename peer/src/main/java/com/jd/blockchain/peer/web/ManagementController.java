package com.jd.blockchain.peer.web;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import bftsmart.reconfiguration.Reconfiguration;
import bftsmart.reconfiguration.ReconfigureReply;
import bftsmart.reconfiguration.util.HostsConfig;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.reconfiguration.views.MemoryBasedViewStorage;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.ServiceProxy;
import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.*;
import com.jd.blockchain.consensus.bftsmart.*;
import com.jd.blockchain.crypto.*;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.service.TransactionBatchResultHandle;
import com.jd.blockchain.transaction.SignatureUtils;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.transaction.TxRequestMessage;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.blockchain.utils.PropertiesUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.ledger.core.TransactionSetQuery;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.ledger.proof.MerkleData;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.peer.consensus.LedgerStateManager;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.net.NetworkAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.action.ActionResponse;
import com.jd.blockchain.consensus.mq.server.MsgQueueMessageDispatcher;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.LedgerManage;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.peer.ConsensusRealm;
import com.jd.blockchain.peer.LedgerBindingConfigAware;
import com.jd.blockchain.peer.PeerManage;
import com.jd.blockchain.setting.GatewayIncomingSetting;
import com.jd.blockchain.setting.LedgerIncomingSetting;
import com.jd.blockchain.storage.service.DbConnection;
import com.jd.blockchain.storage.service.DbConnectionFactory;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig.BindingConfig;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.io.ByteArray;
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

	private static Properties systemConfig;

	private int viewId;

	@Autowired
	private LedgerManage ledgerManager;

	@Autowired
	private DbConnectionFactory connFactory;

	private Map<HashDigest, MsgQueueMessageDispatcher> ledgerTxConverters = new ConcurrentHashMap<>();

	private Map<HashDigest, NodeServer> ledgerPeers = new ConcurrentHashMap<>();

	private Map<HashDigest, CryptoSetting> ledgerCryptoSettings = new ConcurrentHashMap<>();

	private Map<HashDigest, AsymmetricKeypair> ledgerKeypairs = new ConcurrentHashMap<>();

	private Map<HashDigest, ParticipantNode> ledgerCurrNodes = new ConcurrentHashMap<>();

	private Map<HashDigest, LedgerQuery> ledgerQuerys = new ConcurrentHashMap<>();

	private LedgerBindingConfig config;

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
		DataContractRegistry.register(EventPublishOperation.class);
		DataContractRegistry.register(EventPublishOperation.EventEntry.class);

		DataContractRegistry.register(Operation.class);
		DataContractRegistry.register(ContractCodeDeployOperation.class);
		DataContractRegistry.register(ContractEventSendOperation.class);
		DataContractRegistry.register(DataAccountRegisterOperation.class);
		DataContractRegistry.register(EventAccountRegisterOperation.class);
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
                if (authId.getProviderName() == null ||
                        authId.getProviderName().length() <= 0 ||
                        !authId.getProviderName().equalsIgnoreCase(peerProviderName)) {
                    continue;
                }
                try {
                    clientIncomingSettings = peer.getConsensusManageService().authClientIncoming(authId);
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

		ledgerQuerys.put(ledgerHash, ledgerRepository);

		LedgerAdminInfo ledgerAdminAccount = ledgerRepository.getAdminInfo();

		ConsensusProvider provider = getProvider(ledgerAdminAccount);

		// load consensus setting;
		ConsensusSettings csSettings = getConsensusSetting(ledgerAdminAccount);

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

		NodeServer server = null;

		// 处于ACTIVED状态的参与方才会创建共识节点
		if (currentNode.getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {

			ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(ledgerHash.toBase58(),
					csSettings, currentNode.getAddress().toString());

			((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());

			server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
					consensusStateManager);
			ledgerPeers.put(ledgerHash, server);
		}

		ledgerCryptoSettings.put(ledgerHash, ledgerAdminAccount.getSettings().getCryptoSetting());
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
	@RequestMapping(path = "/delegate/activeparticipant", method = RequestMethod.POST)
	public TransactionResponse activateParticipant(@RequestParam("ledgerHash") String base58LedgerHash) {
		HashDigest remoteNewBlockHash;
		TransactionResponse transactionResponse = new TxResponseMessage();
		HashDigest ledgerHash = new HashDigest(Base58Utils.decode(base58LedgerHash));
		try {

			// 由本节点准备交易
			TransactionRequest txRequest = prepareTx(ledgerHash);

			LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);

			systemConfig = PropertiesUtils.createProperties(((BftsmartConsensusSettings)getConsensusSetting(ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()))).getSystemConfigs());

			viewId = ((BftsmartConsensusSettings) getConsensusSetting(ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()))).getViewId();

			// 验证本参与方是否已经被注册，没有被注册的参与方不能进行状态更新
			if (!verifyState(ledgerRepo)) {
				((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.SUCCESS);
				return txResponseWrapper(transactionResponse);
			}

			// 为交易添加本节点的签名信息，防止无法通过安全策略检查
			txRequest = addNodeSigner(txRequest);

			// 连接原有的共识网络,把交易提交到目标账本的原有共识网络进行共识，即在原有共识网络中执行新参与方的状态激活操作
			TransactionResponse txResponse = commitTxToOrigConsensus(ledgerRepo, txRequest);

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

			if (txResponse.isSuccess()) {
				// 更新原有共识网络节点以及本地新启动节点的视图ID，并启动本地新节点的共识服务
				TransactionState transactionState = updateViewAndStartServer(ledgerRepo);
				((TxResponseMessage)txResponse).setExecutionState(transactionState);
			}
			return txResponse;

		} catch (ViewUpdateException e) {
			LOGGER.error("[ManagementController] view update exception!");
			return null;

		} catch (StartServerException e) {
			LOGGER.error("[ManagementController] start server exception!");
			return null;
		}
	}

	// 在指定的账本上准备一笔激活参与方状态的操作
	private TransactionRequest prepareTx(HashDigest ledgerHash) {

			TxBuilder txbuilder = new TxBuilder(ledgerHash);

			txbuilder.states().update(new BlockchainIdentityData(ledgerKeypairs.get(ledgerHash).getPubKey()), ParticipantNodeState.CONSENSUS);

			TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

		    reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(), ledgerKeypairs.get(ledgerHash).getPrivKey()));

		    return reqBuilder.buildRequest();

	}

	private TransactionState updateViewAndStartServer(LedgerRepository ledgerRepository) {

		ParticipantNodeState currNodeLastState = null;
		ParticipantNodeState currNodeNewState = null;

		try {
			ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());

			ParticipantNode[] lastBlockParticipants = ledgerRepository.getAdminInfo(ledgerRepository.getBlock(ledgerRepository.retrieveLatestBlockHeight() - 1)).getParticipants();

			// 检查本参与方以前的共识状态
			for(ParticipantNode participantNode : lastBlockParticipants) {
				if (participantNode.getAddress().toString().equals(currNode.getAddress().toString())) {
					currNodeLastState = participantNode.getParticipantNodeState();
					break;
				}
			}

			ParticipantNode[] newBlockParticipants = ledgerRepository.getAdminInfo(ledgerRepository.retrieveLatestBlock()).getParticipants();

			// 检查本参与方当前的共识状态
			for(ParticipantNode participantNode : newBlockParticipants) {
				if (participantNode.getAddress().toString().equals(currNode.getAddress().toString())) {
					currNodeNewState = participantNode.getParticipantNodeState();
					break;
				}
			}

			if (currNodeLastState != null && currNodeNewState != null ) {
				// 如果参与方的状态由 false 变为 true ，则创建对应的共识节点，更新共识视图加入共识网络；
				if (currNodeLastState.CODE == ParticipantNodeState.READY.CODE && currNodeNewState.CODE == ParticipantNodeState.CONSENSUS.CODE) {
					View newView = updateView(ledgerRepository);
					// 启动共识节点
					if (newView != null) {
						LOGGER.info("[ManagementController] updateView SUCC!");
						setupServer(ledgerRepository);
					}
				} else if (currNodeLastState.CODE == ParticipantNodeState.CONSENSUS.CODE && currNodeNewState.CODE == ParticipantNodeState.READY.CODE) {
					// 如果参与方的状态由 true 变为 false，则停止节点，更新共识视图从共识网络移除节点；
				} else {
					// 不做任何操作；
				}
			}
		} catch (ViewUpdateException e) {
			throw new ViewUpdateException("[ManagementController] view update exception!");
		} catch (StartServerException e) {
			throw new StartServerException("[ManagementController] start server exception!");
		}

		return TransactionState.SUCCESS;
	}

	private boolean verifyState(LedgerRepository ledgerRepo) {
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepo.getHash());

		for (ParticipantNode participantNode : ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()).getParticipants()) {
			if ((participantNode.getAddress().toString().equals(currNode.getAddress().toString())) && participantNode.getParticipantNodeState() == ParticipantNodeState.READY) {
				return true;
			}
		}
		// 参与方的状态已经处于激活状态，不需要再激活
		LOGGER.info("Participant state has been activated, no need be activated repeatedly!");
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

	// 视图更新完成，启动共识节点
	private void setupServer(LedgerRepository ledgerRepository) {
		try {

			ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());

			LedgerAdminInfo ledgerAdminAccount = ledgerRepository.getAdminInfo(ledgerRepository.getBlock(ledgerRepository.retrieveLatestBlockHeight()));

			// load provider;
			ConsensusProvider provider = getProvider(ledgerAdminAccount);

			// load consensus setting;
			ConsensusSettings csSettings = getConsensusSetting(ledgerAdminAccount);

			ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(ledgerRepository.getHash().toBase58(),
					csSettings, currNode.getAddress().toString());

			((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());

			NodeServer server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
					consensusStateManager);

			ledgerPeers.put(ledgerRepository.getHash(), server);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runRealm(server);

			LOGGER.info("[ManagementController] setupServer SUCC!");
		} catch (Exception e) {
			throw new StartServerException("[ManagementController] start server fail exception");
		}

	}

	// 通知原有的共识网络更新共识的视图ID
	private View updateView(LedgerRepository ledgerRepository) {
		NetworkAddress newPeer = null;
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());
		LedgerAdminInfo ledgerAdminInfo = ledgerRepository.getAdminInfo(ledgerRepository.retrieveLatestBlock());

		LOGGER.info("ManagementController start updateView operation!");

		try {

			// load consensus setting
			ConsensusSettings csSettings = getConsensusSetting(ledgerAdminInfo);

			// 找到当前参与方对应的共识网络配置
			for (NodeSettings nodeSettings : csSettings.getNodes()) {
				if (nodeSettings.getAddress().equals(currNode.getAddress().toString())) {
					newPeer = ((BftsmartNodeSettings)nodeSettings).getNetworkAddress();
					break;
				}
			}

			ServiceProxy  peerProxy = createPeerProxy(ledgerRepository);

			Reconfiguration reconfiguration = new Reconfiguration(peerProxy.getProcessId(), peerProxy);

			// addServer的第一个参数指待加入共识的新参与方的编号
			reconfiguration.addServer(currNode.getId(), newPeer.getHost(), newPeer.getPort());

			// 执行更新目标共识网络的视图ID
			ReconfigureReply reconfigureReply = reconfiguration.execute();

			// 返回新视图
			return reconfigureReply.getView();

		} catch (Exception e) {
			throw new ViewUpdateException("[ManagementController] view update fail exception!");
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
				LOGGER.error("[ManagementController] Activate local participant state, write local ledger, but new block hash is inconsistent with remote consensus network!");
				throw new IllegalStateException("[ManagementController] Activate local participant state, write local ledger, but new block hash is inconsistent with remote consensus network!");
			}
		} catch (Exception e) {
			handle.cancel(LEDGER_ERROR);
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.LEDGER_ERROR);
			e.printStackTrace();
			return transactionResponse;
		}

		LOGGER.warn("[ManagementController] commitTxToLocalLedger SUCC!");

		return txResponseWrapper(handle.getResponses().next());
	}

	private ServiceProxy createPeerProxy(LedgerRepository ledgerRepository) {

		HostsConfig hostsConfig;
		List<NodeSettings> origConsensusNodes;
		List<HostsConfig.Config> configList = new ArrayList<>();
		List<InetSocketAddress> nodeAddresses = new ArrayList<>();

		try {
			// 排除未激活的共识节点，找到处于激活状态的共识节点，也就是新参与方注册前的原有共识网络
			origConsensusNodes = SearchOrigConsensusNodes(ledgerRepository);

			int[] origConsensusProcesses = new int[origConsensusNodes.size()];

			for (int i = 0; i < origConsensusNodes.size(); i++) {
				BftsmartNodeSettings node = (BftsmartNodeSettings) origConsensusNodes.get(i);
				origConsensusProcesses[i] = node.getId();
				configList.add(new HostsConfig.Config(node.getId(), node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort()));
				nodeAddresses.add(new InetSocketAddress(node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort()));
			}

			// 构建共识的代理客户端需要的主机配置和系统参数配置结构
			hostsConfig = new HostsConfig(configList.toArray(new HostsConfig.Config[configList.size()]));

			Properties tempSystemConfig = (Properties) systemConfig.clone();

			// 构建tom 配置
			TOMConfiguration tomConfig = new TOMConfiguration((int) -System.nanoTime(), tempSystemConfig, hostsConfig);

			// 根据配置信息构建客户端视图，VIEW ID没有持久化，所以不能确定原有共识网络中的视图信息，在这里先使用默认的0
			View view = new View(viewId, origConsensusProcesses, tomConfig.getF(), nodeAddresses.toArray(new InetSocketAddress[nodeAddresses.size()]));

			// 构建共识的代理客户端，连接目标共识节点，并递交交易进行共识过程
			return new ServiceProxy(tomConfig, new MemoryBasedViewStorage(view), null, null);

		} catch (Exception e) {
			e.printStackTrace();
			throw new CreateProxyClientException("[ManagementController] create proxy client exception!");
		}

	}

	// SDK 通过Peer节点转发交易到远端的共识网络
	private TransactionResponse commitTxToOrigConsensus(LedgerRepository ledgerRepository, TransactionRequest txRequest) {
		TransactionResponse transactionResponse = new TxResponseMessage();

		ServiceProxy peerProxy = createPeerProxy(ledgerRepository);

		byte[] result = peerProxy.invokeOrdered(BinaryProtocol.encode(txRequest, TransactionRequest.class));

		if (result == null) {
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.CONSENSUS_NO_REPLY_ERROR);
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

	private ConsensusSettings getConsensusSetting(LedgerAdminInfo ledgerAdminInfo) {

		ConsensusProvider provider = getProvider(ledgerAdminInfo);

		// load consensus setting
		Bytes csSettingBytes = ledgerAdminInfo.getSettings().getConsensusSetting();
		ConsensusSettings csSettings = provider.getSettingsFactory().getConsensusSettingsEncoder()
				.decode(csSettingBytes.toBytes());

		return csSettings;
	}

	ParticipantNodeState getParticipantState(String address, LedgerAdminInfo ledgerAdminInfo) {
		ParticipantNodeState nodeState = null;
		for (ParticipantNode participantNode : ledgerAdminInfo.getParticipants()) {
			if (participantNode.getAddress().toString().equals(address)) {
				nodeState = participantNode.getParticipantNodeState();
				break;
			}
		}

		return nodeState;
	}

	// 查找原有共识网络中的共识节点信息
	private List<NodeSettings> SearchOrigConsensusNodes(LedgerRepository ledgerRepository) {

		List<NodeSettings> origConsensusNodes = new ArrayList<>();

		LedgerAdminInfo ledgerAdminInfo = ledgerRepository.getAdminInfo(ledgerRepository.retrieveLatestBlock());

		// load consensus setting
		ConsensusSettings csSettings = getConsensusSetting(ledgerAdminInfo);

		NodeSettings[] nodeSettingsArray = csSettings.getNodes();
		for (NodeSettings nodeSettings : nodeSettingsArray) {
			// 排除正在进行激活操作的本节点
			if (nodeSettings.getAddress().equals(ledgerCurrNodes.get(ledgerRepository.getHash()).getAddress().toString())) {
				continue;
			}
			// 排除不处于激活状态的其他共识节点
			if (getParticipantState(nodeSettings.getAddress(), ledgerAdminInfo) != ParticipantNodeState.CONSENSUS) {
				continue;
			}

			origConsensusNodes.add(nodeSettings);
		}
		return origConsensusNodes;
	}

}
