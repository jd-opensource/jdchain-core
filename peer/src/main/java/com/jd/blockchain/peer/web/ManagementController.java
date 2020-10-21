package com.jd.blockchain.peer.web;

import bftsmart.reconfiguration.Reconfiguration;
import bftsmart.reconfiguration.ReconfigureReply;
import bftsmart.reconfiguration.util.HostsConfig;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.reconfiguration.views.MemoryBasedViewStorage;
import bftsmart.reconfiguration.views.NodeNetwork;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.ServiceProxy;
import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.ledger.core.*;
import com.jd.blockchain.ledger.merkletree.HashBucketEntry;
import com.jd.blockchain.ledger.merkletree.KeyIndex;
import com.jd.blockchain.ledger.merkletree.MerkleIndex;
import com.jd.blockchain.ledger.proof.MerkleKey;
import com.jd.blockchain.sdk.converters.ClientResolveUtil;
import com.jd.blockchain.sdk.service.PeerBlockchainServiceFactory;
import com.jd.blockchain.service.TransactionBatchResultHandle;
import com.jd.blockchain.transaction.SignatureUtils;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.transaction.TxContentBlob;
import com.jd.blockchain.transaction.TxRequestBuilder;
import com.jd.blockchain.transaction.TxRequestMessage;
import com.jd.blockchain.transaction.TxResponseMessage;
import com.jd.blockchain.utils.PropertiesUtils;

import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.ACTIVE_PARTICIPANT_ID_KEY;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.CONSENSUS_HOST_PATTERN;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.CONSENSUS_PORT_PATTERN;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.CONSENSUS_SECURE_PATTERN;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.DEACTIVE_PARTICIPANT_ID_KEY;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.F_NUM_KEY;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.PARTICIPANT_OP_KEY;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.PUBKEY_PATTERN;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.SERVER_NUM_KEY;
import static com.jd.blockchain.consensus.bftsmart.BftsmartConsensusSettingsBuilder.SERVER_VIEW_KEY;
import static com.jd.blockchain.ledger.TransactionState.LEDGER_ERROR;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.peer.consensus.LedgerStateManager;
import com.jd.blockchain.utils.Property;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.net.NetworkAddress;
import com.jd.blockchain.utils.web.model.WebResponse;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentityData;
import com.jd.blockchain.ledger.ConsensusSettingsUpdateOperation;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.ledger.CreateProxyClientException;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.EventAccountRegisterOperation;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerInitOperation;
import com.jd.blockchain.ledger.LedgerMetadata_V2;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.ParticipantNodeState;
import com.jd.blockchain.ledger.ParticipantRegisterOperation;
import com.jd.blockchain.ledger.ParticipantStateUpdateOperation;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.RoleInitSettings;
import com.jd.blockchain.ledger.RoleSet;
import com.jd.blockchain.ledger.RolesConfigureOperation;
import com.jd.blockchain.ledger.SecurityInitSettings;
import com.jd.blockchain.ledger.StartServerException;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionRequestBuilder;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.UserAuthInitSettings;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.ViewUpdateException;

import com.jd.blockchain.ledger.proof.MerkleTrieData;
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

	public static final  String  BFTSMART_PROVIDER = "com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider";

	public static final String GATEWAY_PUB_EXT_NAME = ".gw.pub";

	private static final String DEFAULT_HASH_ALGORITHM = "SHA256";

	public static final int MIN_GATEWAY_ID = 10000;

	private static Properties systemConfig;

	private int viewId;

	private static List<NodeSettings> origConsensusNodes;


	@Autowired
	private LedgerManage ledgerManager;

	@Autowired
	private DbConnectionFactory connFactory;

//	private Map<HashDigest, MsgQueueMessageDispatcher> ledgerTxConverters = new ConcurrentHashMap<>();

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
		DataContractRegistry.register(TransactionRequest.class);
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
		DataContractRegistry.register(ConsensusSettingsUpdateOperation.class);

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
		DataContractRegistry.register(MerkleTrieData.class);
		DataContractRegistry.register(MerkleKey.class);
		DataContractRegistry.register(MerkleLeaf.class);
		DataContractRegistry.register(MerklePath.class);
		DataContractRegistry.register(MerkleIndex.class);
		DataContractRegistry.register(KeyIndex.class);
		DataContractRegistry.register(HashBucketEntry.class);

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
		LedgerQuery ledgerRepository = null;
		NodeServer server = null;
		ParticipantNode currentNode = null;
		LedgerAdminInfo ledgerAdminAccount = null;

		try {
			DbConnection dbConnNew = connFactory.connect(bindingConfig.getDbConnection().getUri(),
					bindingConfig.getDbConnection().getPassword());
			ledgerRepository = ledgerManager.register(ledgerHash, dbConnNew.getStorageService());

			ledgerAdminAccount = ledgerRepository.getAdminInfo();

			ConsensusProvider provider = getProvider(ledgerAdminAccount);

			// load consensus setting;
			ConsensusSettings csSettings = getConsensusSetting(ledgerAdminAccount);

			// find current node;

			for (ParticipantNode participantNode : ledgerAdminAccount.getParticipants()) {
				if (participantNode.getAddress().toString().equals(bindingConfig.getParticipant().getAddress())) {
					currentNode = participantNode;
				}
			}
			if (currentNode == null) {
				throw new IllegalArgumentException(
						"Current node is not found from the participant settings of ledger[" + ledgerHash.toBase58() + "]!");
			}

			// 处于ACTIVED状态的参与方才会创建共识节点
			if (currentNode.getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {

				ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(ledgerHash.toBase58(),
						csSettings, currentNode.getAddress().toString());

				((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());

				server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
						consensusStateManager);
				ledgerPeers.put(ledgerHash, server);
			}

		} catch (Exception e) {
			ledgerManager.unregister(ledgerHash);
			throw e;
		}

		ledgerQuerys.put(ledgerHash, ledgerRepository);
		ledgerCurrNodes.put(ledgerHash, currentNode);
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
	 * @param base58LedgerHash
	 *              base58格式的账本哈希；
	 * @param consensusHost
	 *              激活参与方的共识Ip
	 * @param consensusPort
	 *              激活参与方的共识Port
	 * @param remoteManageHost
	 * 	            提供完备数据库的共识节点管理IP
	 * @param remoteManagePort
	 * 	            提供完备数据库的共识节点管理Port
	 * @return
	 */
	@RequestMapping(path = "/delegate/activeparticipant", method = RequestMethod.POST)
	public WebResponse activateParticipant(@RequestParam("ledgerHash") String base58LedgerHash, @RequestParam("consensusHost") String consensusHost, @RequestParam("consensusPort") String consensusPort, @RequestParam("remoteManageHost") String remoteManageHost, @RequestParam("remoteManagePort") String remoteManagePort) {
		TransactionResponse remoteTxResponse;
		WebResponse webResponse;
		TransactionBatchResultHandle handle = null;
		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();

		try {
			HashDigest ledgerHash = new HashDigest(Base58Utils.decode(base58LedgerHash));

			if (ledgerKeypairs.get(ledgerHash) == null) {
				return WebResponse.createFailureResult(-1, "[ManagementController] input ledgerhash not exist!");
			}

			LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);

			LedgerAdminInfo ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock());

			if (ledgerAdminInfo.getSettings().getConsensusProvider().equals(BFTSMART_PROVIDER)) {

				// 检查本地节点与远端节点在库上是否存在差异,有差异的话需要进行差异交易重放
				webResponse = checkLedgerDiff(ledgerRepo, ledgerKeypairs.get(ledgerHash), remoteManageHost, remoteManagePort);

				if (!webResponse.isSuccess()) {
					return webResponse;
				}

				ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock());

				ParticipantNode[] participants = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()).getParticipants();

				systemConfig = PropertiesUtils.createProperties(((BftsmartConsensusSettings) getConsensusSetting(ledgerAdminInfo)).getSystemConfigs());

				viewId = ((BftsmartConsensusSettings) getConsensusSetting(ledgerAdminInfo)).getViewId();

				// 由本节点准备交易

				TransactionRequest txRequest = prepareActiveTx(ledgerHash, participants, consensusHost, consensusPort);

				// 验证本参与方是否已经被注册，没有被注册的参与方不能进行状态更新
				if (!verifyState(ledgerRepo, Op.ACTIVE)) {
					return WebResponse.createSuccessResult(null);
				}

				origConsensusNodes = SearchOrigConsensusNodes(ledgerRepo);

				// 为交易添加本节点的签名信息，防止无法通过安全策略检查
				txRequest = addNodeSigner(txRequest);

				// 在本地账本执行交易；
				// 验证本地区块与远程区块是否一致，如果不一致，本地回滚，返回失败结果；
				// 如果区块一致，提交区块；
				TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(ledgerRepo, opReg);

				txbatchProcessor.schedule(txRequest);

				// 连接原有的共识网络,把交易提交到目标账本的原有共识网络进行共识，即在原有共识网络中执行新参与方的状态激活操作
				remoteTxResponse = commitTxToOrigConsensus(ledgerRepo, txRequest);

				// 保证原有共识网络账本状态与共识协议的视图更新信息一致
				long blockGenerateTime = remoteTxResponse.getBlockGenerateTime();
				if (remoteTxResponse.isSuccess()) {
					try {
						View newView = updateView(ledgerRepo, consensusHost, Integer.parseInt(consensusPort), Op.ACTIVE);
						if (newView != null && newView.isMember(ledgerCurrNodes.get(ledgerRepo.getHash()).getId())) {
							LOGGER.info("[ManagementController] updateView SUCC!");
						} else if (newView == null) {
							throw new IllegalStateException("[ManagementController] client recv response timeout, consensus may be stalemate, please restart all nodes!");
						}
					} catch (Exception e) {
						cancelBlock(blockGenerateTime, txbatchProcessor);
						return WebResponse.createFailureResult(-1, "[ManagementController] commit tx to orig consensus, tx execute succ but view update failed, please restart all nodes and copy database for new participant node!");
					}
				} else {
					cancelBlock(remoteTxResponse.getBlockGenerateTime(), txbatchProcessor);
					return WebResponse.createFailureResult(-1, "[ManagementController] commit tx to orig consensus, tx execute failed, please retry activate participant!");
				}
				// 进行Prepare
				LedgerEditor.TIMESTAMP_HOLDER.set(blockGenerateTime);
				handle = txbatchProcessor.prepare();
				if (handle.getBlock().getHash().toBase58().equals(remoteTxResponse.getBlockHash().toBase58())) {
					handle.commit();
				} else {
					handle.cancel(LEDGER_ERROR);
					return WebResponse.createFailureResult(-1, "[ManagementController] activate local participant state, write local ledger, but new block hash is inconsistent with remote consensus network!");
				}

				setupServer(ledgerRepo);

				return WebResponse.createSuccessResult(null);

			} else {
				//Todo
				//mq or others
				return WebResponse.createSuccessResult(null);
			}

		} catch (Exception e) {
			return WebResponse.createFailureResult(-1, "[ManagementController] activate new particpant failed!" + e);
		}
	}

	private void cancelBlock(long blockGenerateTime, TransactionBatchProcessor txBatchProcessor) {
		LedgerEditor.TIMESTAMP_HOLDER.set(blockGenerateTime);
		TransactionBatchResultHandle handle = txBatchProcessor.prepare();
		handle.cancel(LEDGER_ERROR);
	}

	/**
	 * 代理交易； <br>
	 *
	 * 此方法假设当前节点是一个待移除的共识节点, 通过此方法接收一笔用于实现管理操作的交易；
	 *
	 * <p>
	 *
	 * 此方法接收到交易之后，先把交易提交到已有的共识网络执行，这个已有网络包括本节点； <br>
	 *
	 * <p>
	 * 如果操作中涉及到共识参与方的共识参数变化，将触发将此节点的共识拓扑改变的操作；
	 *
	 * @param base58LedgerHash
	 *              base58格式的账本哈希；
	 * @param participantAddress
	 *              待移除参与方的地址
	 * @param remoteManageHost
	 *              提供完备数据库的共识节点管理IP
	 * @param remoteManagePort
	 *              提供完备数据库的共识节点管理Port
	 * @return
	 */
	@RequestMapping(path = "/delegate/deactiveparticipant", method = RequestMethod.POST)
	public WebResponse deActivateParticipant(@RequestParam("ledgerHash") String base58LedgerHash, @RequestParam("participantAddress") String participantAddress, @RequestParam("remoteManageHost") String remoteManageHost, @RequestParam("remoteManagePort") String remoteManagePort) {
		TransactionResponse txResponse;
		WebResponse webResponse;

		try {
			HashDigest ledgerHash = new HashDigest(Base58Utils.decode(base58LedgerHash));

			// 进行一系列安全检查
			if (ledgerQuerys.get(ledgerHash) == null) {
				return WebResponse.createFailureResult(-1, "[ManagementController] input ledgerhash not exist!");
			}

			if (!ledgerCurrNodes.get(ledgerHash).getAddress().toBase58().equals(participantAddress)) {
				return WebResponse.createFailureResult(-1, "[ManagementController] deactive participant not me!");
			}

			LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);

			LedgerAdminInfo ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock());


			if (ledgerAdminInfo.getSettings().getConsensusProvider().equals(BFTSMART_PROVIDER)) {

				// 检查本地节点与远端节点在库上是否存在差异,有差异的话需要进行差异交易重放
				webResponse = checkLedgerDiff(ledgerRepo, ledgerKeypairs.get(ledgerHash), remoteManageHost, remoteManagePort);

				if (!webResponse.isSuccess()) {
					return webResponse;
				}

				ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock());
				ParticipantNode[] participants = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()).getParticipants();

				// 已经处于最小节点数环境的共识网络，不能再执行去激活操作
				int count = 0;
				for (ParticipantNode participantNode : participants) {
					if (participantNode.getParticipantNodeState().CODE == ParticipantNodeState.CONSENSUS.CODE) {
						count++;
					}
				}

				if (count <= 4) {
					return WebResponse.createFailureResult(-1, "[ManagementController] in minimum number of nodes scenario, deactive op is not allowed!");
				}

				systemConfig = PropertiesUtils.createProperties(((BftsmartConsensusSettings) getConsensusSetting(ledgerAdminInfo)).getSystemConfigs());

				viewId = ((BftsmartConsensusSettings) getConsensusSetting(ledgerAdminInfo)).getViewId();

				// 由本节点准备交易
				TransactionRequest txRequest = prepareDeActiveTx(ledgerHash, participants);

				// 验证本参与方是否已经被注册，没有被注册的参与方不能进行状态更新, 处于激活状态的参与方才能去激活
				if (!verifyState(ledgerRepo, Op.DEACTIVE)) {
					return WebResponse.createSuccessResult(null);
				}

				origConsensusNodes = SearchOrigConsensusNodes(ledgerRepo);

				// 为交易添加本节点的签名信息，防止无法通过安全策略检查
				txRequest = addNodeSigner(txRequest);

				// 连接原有的共识网络,把交易提交到目标账本的原有共识网络进行共识，即在原有共识网络中执行参与方的去激活操作，这个原有网络包括本节点
				txResponse = commitTxToOrigConsensus(ledgerRepo, txRequest);

				// 保证原有共识网络账本状态与共识协议的视图更新信息一致
				if (txResponse.isSuccess()) {

					try {
						View newView = updateView(ledgerRepo, null, -1, Op.DEACTIVE);
						if (newView != null && !newView.isMember(ledgerCurrNodes.get(ledgerRepo.getHash()).getId())) {
							LOGGER.info("[ManagementController] updateView SUCC!");
						} else if (newView == null) {
							throw new IllegalStateException("[ManagementController] client recv response timeout, consensus may be stalemate, please restart all nodes!");
						}
					} catch (Exception e) {
						return WebResponse.createFailureResult(-1, "[ManagementController] commit tx to orig consensus, tx execute succ but view update failed, please restart all nodes to keep ledger and protocal is consistent about view info!");
					}
				} else {
					return WebResponse.createFailureResult(-1, "[ManagementController] commit tx to orig consensus, tx execute failed, please retry deactivate participant!");
				}

				return WebResponse.createSuccessResult(null);

			} else {
				//Todo
				//mq or others
				return WebResponse.createSuccessResult(null);
			}

		} catch (Exception e) {
			return WebResponse.createFailureResult(-1, "[ManagementController] deactivate particpant failed!" + e);
		}
	}

	private TransactionRequest prepareDeActiveTx(HashDigest ledgerHash, ParticipantNode[] participants) {

		PubKey deActivePubKey = ledgerKeypairs.get(ledgerHash).getPubKey();
		int deActiveID = 0;

		for(int i = 0; i < participants.length; i++) {
			if (deActivePubKey.equals(participants[i].getPubKey())) {
				deActiveID = participants[i].getId();
				break;
			}
		}

		// organize system config properties
		Property[] properties = createDeactiveProperties(deActivePubKey, deActiveID);

		TxBuilder txbuilder = new TxBuilder(ledgerHash, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());

		// This transaction contains participant state update and settings update two ops
		txbuilder.states().update(new BlockchainIdentityData(deActivePubKey), ParticipantNodeState.DECONSENSUS);

		txbuilder.settings().update(properties);

		TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

		reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(), ledgerKeypairs.get(ledgerHash).getPrivKey()));

		return reqBuilder.buildRequest();

	}

	private WebResponse checkLedgerDiff(LedgerRepository ledgerRepository, AsymmetricKeypair localKeyPair, String remoteManageHost, String remoteManagePort) {

		List<String> providers = new ArrayList<String>();

		long localLatestBlockHeight = ledgerRepository.getLatestBlockHeight();

		HashDigest localLatestBlockHash = ledgerRepository.getLatestBlockHash();

		HashDigest remoteBlockHash;

		long remoteLatestBlockHeight = -1; // 激活新节点时，远端管理节点最新区块高度

		HashDigest ledgerHash = ledgerRepository.getHash();

		TransactionBatchResultHandle handle = null;

		OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();

		try {
			providers.add(BFTSMART_PROVIDER);

			PeerBlockchainServiceFactory blockchainServiceFactory = PeerBlockchainServiceFactory.connect(localKeyPair, new NetworkAddress(remoteManageHost, Integer.parseInt(remoteManagePort)), providers);

			remoteLatestBlockHeight = blockchainServiceFactory.getBlockchainService().getLedger(ledgerHash).getLatestBlockHeight();

			if ((localLatestBlockHeight <= remoteLatestBlockHeight)) {
				// 检查本节点与拉取节点相同高度的区块，哈希是否一致,不一致说明其中一个节点的数据库被污染了
				remoteBlockHash = blockchainServiceFactory.getBlockchainService().getBlock(ledgerHash, localLatestBlockHeight).getHash();

				if (!(localLatestBlockHash.toBase58().equals(remoteBlockHash.toBase58()))){
					throw new IllegalStateException("[ManagementController] checkLedgerDiff, ledger database is inconsistent, please check ledger database!");
				}
				// 本节点与拉取节点高度一致，不需要进行交易重放
				if (localLatestBlockHeight == remoteLatestBlockHeight) {
					return WebResponse.createSuccessResult(null);
				}
			} else {
				throw new IllegalStateException("[ManagementController] checkLedgerDiff, local latest block height > remote node latest block height!");
			}

			// 对差异进行交易重放
			for (int height = (int)localLatestBlockHeight + 1; height <= remoteLatestBlockHeight; height++) {
				TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(ledgerRepository, opReg);
				// transactions replay
				try {
					HashDigest pullBlockHash = blockchainServiceFactory.getBlockchainService().getBlock(ledgerHash, height).getHash();
					long pullBlockTime = blockchainServiceFactory.getBlockchainService().getBlock(ledgerHash, height).getTimestamp();

					//获取区块内的增量交易
					LedgerTransaction[] addition_transactions = blockchainServiceFactory.getBlockchainService().getAdditionalTransactions(ledgerHash, height, 0, -1);

					for (LedgerTransaction ledgerTransaction : addition_transactions) {

						TxContentBlob txContentBlob = new TxContentBlob(ledgerHash);

						txContentBlob.setTime(ledgerTransaction.getRequest().getTransactionContent().getTimestamp());

						// convert operation, from json to object
						for (Operation operation : ledgerTransaction.getRequest().getTransactionContent().getOperations()) {
							txContentBlob.addOperation(ClientResolveUtil.read(operation));
						}

						TxRequestBuilder txRequestBuilder = new TxRequestBuilder(ledgerTransaction.getTransactionHash(), txContentBlob);
						txRequestBuilder.addNodeSignature(ledgerTransaction.getRequest().getNodeSignatures());
						txRequestBuilder.addEndpointSignature(ledgerTransaction.getRequest().getEndpointSignatures());
						TransactionRequest transactionRequest = txRequestBuilder.buildRequest();
						txbatchProcessor.schedule(transactionRequest);
					}

					LedgerEditor.TIMESTAMP_HOLDER.set(pullBlockTime);
					handle = txbatchProcessor.prepare();

					if (!(handle.getBlock().getHash().toBase58().equals(pullBlockHash.toBase58()))) {
						LOGGER.error("[ManagementController] checkLedgerDiff, transactions replay result is inconsistent at height {}", height);
						throw new IllegalStateException("[ManagementController] checkLedgerDiff, transactions replay, block hash result is inconsistent!");
					}

					handle.commit();

				} catch (Exception e) {
					handle.cancel(LEDGER_ERROR);
					throw new IllegalStateException("[ManagementController] checkLedgerDiff, transactions replay failed!", e);
				}
			}
		} catch (Exception e) {
			return WebResponse.createFailureResult(-1, "[ManagementController] checkLedgerDiff error!" + e);
		}

		return WebResponse.createSuccessResult(null);
	}

	// order transactions by timestamp in block
//	private LedgerTransaction[] orderTransactions(LedgerTransaction[] transactions) {
//		List<LedgerTransaction> transactionList = Arrays.asList(transactions);
//		transactionList.sort(new Comparator<LedgerTransaction>() {
//			@Override
//			public int compare(LedgerTransaction t1, LedgerTransaction t2) {
//				return (int)(t1.getTransactionContent().getTimestamp() - t2.getTransactionContent().getTimestamp());
//			}
//		});
//		return transactionList.toArray(new LedgerTransaction[transactions.length]);
//	}

	private static String keyOfNode(String pattern, int id) {
		return String.format(pattern, id);
	}

	private String createActiveView(String oldView, int id) {

		StringBuilder views = new StringBuilder(oldView);

		views.append(",");

		views.append(id);

		return views.toString();
	}

	private String createDeactiveView(String oldView, int id) {

		StringBuilder newViews = new StringBuilder("");

		String[] viewIdArray = oldView.split(",");

		for (String viewId : viewIdArray) {
			if (Integer.parseInt(viewId) != id) {
				newViews.append(viewId);
				newViews.append(",");
			}
		}
		String newView = newViews.toString();

		return newView.substring(0, newView.length() - 1);
	}

	// organize active participant related system config properties
	private Property[] createActiveProperties(String host, String port, PubKey activePubKey, int activeID) {
		int oldServerNum = Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY));
		int oldFNum = Integer.parseInt(systemConfig.getProperty(F_NUM_KEY));
		String oldView = systemConfig.getProperty(SERVER_VIEW_KEY);

		List<Property> properties = new ArrayList<Property>();

		properties.add(new Property(keyOfNode(CONSENSUS_HOST_PATTERN, activeID), host));
		properties.add(new Property(keyOfNode(CONSENSUS_PORT_PATTERN, activeID), port));
		properties.add(new Property(keyOfNode(CONSENSUS_SECURE_PATTERN, activeID), "false"));
		properties.add(new Property(keyOfNode(PUBKEY_PATTERN, activeID), activePubKey.toBase58()));
		properties.add(new Property(SERVER_NUM_KEY, String.valueOf(Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY)) + 1)));
		properties.add(new Property(PARTICIPANT_OP_KEY, "active"));
		properties.add(new Property(ACTIVE_PARTICIPANT_ID_KEY,  String.valueOf(activeID)));


		if ((oldServerNum + 1) >= (3*(oldFNum + 1) + 1)) {
			properties.add(new Property(F_NUM_KEY, String.valueOf(oldFNum + 1)));
		}
		properties.add(new Property(SERVER_VIEW_KEY, createActiveView(oldView, activeID)));

		return properties.toArray(new Property[properties.size()]);
	}

	// organize deactive participant related system config properties
	private Property[] createDeactiveProperties(PubKey deActivePubKey, int deActiveID) {
		int oldServerNum = Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY));
		int oldFNum = Integer.parseInt(systemConfig.getProperty(F_NUM_KEY));
		String oldView = systemConfig.getProperty(SERVER_VIEW_KEY);

		List<Property> properties = new ArrayList<Property>();

		properties.add(new Property(SERVER_NUM_KEY, String.valueOf(Integer.parseInt(systemConfig.getProperty(SERVER_NUM_KEY)) - 1)));

		if ((oldServerNum - 1) < (3*oldFNum + 1)) {
			properties.add(new Property(F_NUM_KEY, String.valueOf(oldFNum - 1)));
		}
		properties.add(new Property(SERVER_VIEW_KEY, createDeactiveView(oldView, deActiveID)));

		properties.add(new Property(PARTICIPANT_OP_KEY, "deactive"));

		properties.add(new Property(DEACTIVE_PARTICIPANT_ID_KEY,  String.valueOf(deActiveID)));

		return properties.toArray(new Property[properties.size()]);
	}

	// 在指定的账本上准备一笔激活参与方状态及系统配置参数的操作
	private TransactionRequest prepareActiveTx(HashDigest ledgerHash, ParticipantNode[] participants, String host, String port) {

		PubKey activePubKey = ledgerKeypairs.get(ledgerHash).getPubKey();
		int activeID = 0;

		for(int i = 0; i < participants.length; i++) {
			if (activePubKey.equals(participants[i].getPubKey())) {
				activeID = participants[i].getId();
				break;
			}
		}

		// organize system config properties
		Property[] properties = createActiveProperties(host, port, activePubKey, activeID);

		TxBuilder txbuilder = new TxBuilder(ledgerHash, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());

		// This transaction contains participant state update and settings update two ops
		txbuilder.states().update(new BlockchainIdentityData(activePubKey), ParticipantNodeState.CONSENSUS);

		txbuilder.settings().update(properties);

		TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

		reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(), ledgerKeypairs.get(ledgerHash).getPrivKey()));

		return reqBuilder.buildRequest();

	}

	private boolean verifyState(LedgerRepository ledgerRepo, Op op) {
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepo.getHash());

        if (op == Op.ACTIVE) {
			for (ParticipantNode participantNode : ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()).getParticipants()) {
				if ((participantNode.getAddress().toString().equals(currNode.getAddress().toString())) &&  ((participantNode.getParticipantNodeState() == ParticipantNodeState.READY) || (participantNode.getParticipantNodeState() == ParticipantNodeState.DECONSENSUS))) {
					return true;
				}
			}
			// 参与方的状态已经处于激活状态，不需要再激活
			LOGGER.info("Participant state has been activated, no need be activated repeatedly!");
		} else if (op == Op.DEACTIVE) {
			for (ParticipantNode participantNode : ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock()).getParticipants()) {
				if ((participantNode.getAddress().toString().equals(currNode.getAddress().toString())) && participantNode.getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {
					return true;
				}
			}
			// 参与方的状态已经处于激活状态，不需要再激活
			LOGGER.info("Participant state has been deactivated, no need be deactivated repeatedly!");
		}

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
			throw new StartServerException("[ManagementController] start server fail exception", e);
		}

	}

	// 通知原有的共识网络更新共识的视图ID
	private View updateView(LedgerRepository ledgerRepository, String consensusHost, int consensusPort, Op op) {
		ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());

		LOGGER.info("ManagementController start updateView operation!");

		try {
			ServiceProxy  peerProxy = createPeerProxy();

			Reconfiguration reconfiguration = new Reconfiguration(peerProxy.getProcessId(), peerProxy);

			if (op == Op.ACTIVE) {
				// addServer的第一个参数指待加入共识的新参与方的编号
				reconfiguration.addServer(currNode.getId(), consensusHost, consensusPort);
			} else if (op == Op.DEACTIVE) {
				// 参数为待移除共识节点的id
				reconfiguration.removeServer(currNode.getId());
			} else {
				throw new IllegalArgumentException("[ManagementController] op type error!");
			}

			// 执行更新目标共识网络的视图ID
			ReconfigureReply reconfigureReply = reconfiguration.execute();

			peerProxy.close();

			// 返回新视图
			return reconfigureReply.getView();

		} catch (Exception e) {
			throw new ViewUpdateException("[ManagementController] view update fail exception!", e);
		}
	}

	private TransactionRequest addNodeSigner(TransactionRequest txRequest) {
		TxRequestMessage txMessage = new TxRequestMessage(txRequest);

		HashDigest ledgerHash = txRequest.getTransactionContent().getLedgerHash();
		AsymmetricKeypair peerKeypair = ledgerKeypairs.get(ledgerHash);
		DigitalSignature nodeSigner = SignatureUtils.sign(ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm(), txRequest.getTransactionContent(), peerKeypair);

		txMessage.addNodeSignatures(nodeSigner);

		// 计算交易哈希；
//		byte[] nodeRequestBytes = BinaryProtocol.encode(txMessage, TransactionRequest.class);
//		HashFunction hashFunc = Crypto.getHashFunction(cryptoSetting.getHashAlgorithm());
//		HashDigest txHash = hashFunc.hash(nodeRequestBytes);
//		txMessage.setTransactionHash(txHash);

		return txMessage;
	}

	private TransactionResponse txResponseWrapper(TransactionResponse txResponse) {
		return new TxResponseMessage(txResponse, null);
	}

	private ServiceProxy createPeerProxy() {

		HostsConfig hostsConfig;
		List<HostsConfig.Config> configList = new ArrayList<>();
		List<NodeNetwork> nodeAddresses = new ArrayList<>();

		try {

			int[] origConsensusProcesses = new int[origConsensusNodes.size()];

			for (int i = 0; i < origConsensusNodes.size(); i++) {
				BftsmartNodeSettings node = (BftsmartNodeSettings) origConsensusNodes.get(i);
				origConsensusProcesses[i] = node.getId();
				configList.add(new HostsConfig.Config(node.getId(), node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort(), -1));
				nodeAddresses.add(new NodeNetwork(node.getNetworkAddress().getHost(), node.getNetworkAddress().getPort(), -1));
			}

			// 构建共识的代理客户端需要的主机配置和系统参数配置结构
			hostsConfig = new HostsConfig(configList.toArray(new HostsConfig.Config[configList.size()]));

			Properties tempSystemConfig = (Properties) systemConfig.clone();

			// 构建tom 配置
			TOMConfiguration tomConfig = new TOMConfiguration((int) -System.nanoTime(), tempSystemConfig, hostsConfig);

			View view = new View(viewId, origConsensusProcesses, tomConfig.getF(), nodeAddresses.toArray(new NodeNetwork[nodeAddresses.size()]));

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

		ServiceProxy peerProxy = createPeerProxy();

		byte[] result = peerProxy.invokeOrdered(BinaryProtocol.encode(txRequest, TransactionRequest.class));

		if (result == null) {
			((TxResponseMessage) transactionResponse).setExecutionState(TransactionState.CONSENSUS_NO_REPLY_ERROR);
			return transactionResponse;
		}

		peerProxy.close();

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
			// 排除不处于激活状态的共识节点
			if (getParticipantState(nodeSettings.getAddress(), ledgerAdminInfo) != ParticipantNodeState.CONSENSUS) {
				continue;
			}

			origConsensusNodes.add(nodeSettings);
		}
		return origConsensusNodes;
	}

	enum Op {

		ACTIVE,

		DEACTIVE

	}

}
