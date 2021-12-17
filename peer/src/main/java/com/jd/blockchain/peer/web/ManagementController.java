package com.jd.blockchain.peer.web;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.consensus.NodeNetworkAddress;
import com.jd.blockchain.consensus.bftsmart.service.BftsmartNodeState;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.ledger.BlockRollbackException;
import com.jd.blockchain.ledger.ConsensusReconfigOperation;
import com.jd.blockchain.ledger.ConsensusTypeUpdateOperation;
import com.jd.blockchain.ledger.CryptoHashAlgoUpdateOperation;
import com.jd.blockchain.ledger.IdentityMode;
import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.core.MerkleTreeSimple;
import com.jd.blockchain.ledger.core.TransactionSet;
import com.jd.blockchain.ledger.core.UserAccount;
import com.jd.blockchain.peer.service.IParticipantManagerService;
import com.jd.blockchain.peer.service.ParticipantContext;
import com.jd.blockchain.peer.service.ConsensusServiceFactory;
import com.jd.blockchain.sdk.proxy.HttpBlockchainBrowserService;
import com.jd.httpservice.agent.HttpServiceAgent;
import com.jd.httpservice.agent.ServiceConnection;
import com.jd.httpservice.agent.ServiceConnectionManager;
import com.jd.httpservice.agent.ServiceEndpoint;
import utils.BusinessException;
import utils.Bytes;
import utils.Property;
import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.io.ByteArray;
import utils.io.BytesUtils;
import utils.io.Storage;
import utils.net.NetworkAddress;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.action.ActionResponse;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.consensus.service.NodeState;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentityData;
import com.jd.blockchain.ledger.ConsensusSettingsUpdateOperation;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
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
import com.jd.blockchain.ledger.LedgerTransactions;
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
import com.jd.blockchain.ledger.UserAuthInitSettings;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.core.DefaultOperationHandleRegisteration;
import com.jd.blockchain.ledger.core.LedgerEditor;
import com.jd.blockchain.ledger.core.LedgerManage;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerRepository;
import com.jd.blockchain.ledger.core.OperationHandleRegisteration;
import com.jd.blockchain.ledger.core.TransactionBatchProcessor;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.ledger.merkletree.HashBucketEntry;
import com.jd.blockchain.ledger.merkletree.KeyIndex;
import com.jd.blockchain.ledger.merkletree.MerkleIndex;
import com.jd.blockchain.ledger.proof.MerkleKey;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.ledger.proof.MerkleTrieData;
import com.jd.blockchain.peer.ConsensusRealm;
import com.jd.blockchain.peer.LedgerBindingConfigAware;
import com.jd.blockchain.peer.PeerManage;
import com.jd.blockchain.peer.consensus.LedgerStateManager;
import com.jd.blockchain.sdk.AccessSpecification;
import com.jd.blockchain.sdk.GatewayAuthRequest;
import com.jd.blockchain.sdk.ManagementHttpService;
import com.jd.blockchain.service.TransactionBatchResultHandle;
import com.jd.blockchain.setting.GatewayAuthResponse;
import com.jd.blockchain.setting.LedgerIncomingSettings;
import com.jd.blockchain.storage.service.DbConnection;
import com.jd.blockchain.storage.service.DbConnectionFactory;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig.BindingConfig;
import com.jd.blockchain.transaction.SignatureUtils;
import com.jd.blockchain.transaction.TxBuilder;
import com.jd.blockchain.transaction.TxRequestMessage;
import com.jd.blockchain.web.converters.BinaryMessageConverter;
import com.jd.httpservice.utils.web.WebResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utils.net.SSLSecurity;

import javax.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.jd.blockchain.ledger.TransactionState.LEDGER_ERROR;

/**
 * 网关管理服务；
 * <p>
 * 提供
 *
 * @author huanghaiquan
 */
@RestController
@RequestMapping(path = ManagementHttpService.URL_MANAGEMENT)
public class ManagementController implements LedgerBindingConfigAware, PeerManage, ManagementHttpService {

    private static final String STORAGE_CONSENSUS = "consensus";


    private static Logger LOGGER = LoggerFactory.getLogger(ManagementController.class);

    public String DEFAULT_DIR = "";

    public String logDefaultFile;

    @Autowired
    private Storage storage;

    @Autowired
    private LedgerManage ledgerManager;

    @Autowired
    private DbConnectionFactory connFactory;

    private Map<HashDigest, NodeServer> ledgerPeers = new ConcurrentHashMap<>();

    private Map<HashDigest, CryptoSetting> ledgerCryptoSettings = new ConcurrentHashMap<>();

    private Map<HashDigest, AsymmetricKeypair> ledgerKeypairs = new ConcurrentHashMap<>();

    private Map<HashDigest, ParticipantNode> ledgerCurrNodes = new ConcurrentHashMap<>();

    private Map<HashDigest, LedgerQuery> ledgerQuerys = new ConcurrentHashMap<>();

    private Map<HashDigest, IdentityMode> ledgerIdMode = new ConcurrentHashMap<>();

    private Map<HashDigest, SSLSecurity> ledgerSecurities = new ConcurrentHashMap<>();

    @Autowired
    private MessageHandle consensusMessageHandler;

    @Autowired
    private StateMachineReplicate consensusStateManager;

    @Autowired
    private ConsensusServiceFactory consensusServiceFactory;

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
        DataContractRegistry.register(ConsensusReconfigOperation.class);
        DataContractRegistry.register(ConsensusTypeUpdateOperation.class);

        DataContractRegistry.register(CryptoHashAlgoUpdateOperation.class);

        DataContractRegistry.register(ActionResponse.class);

        DataContractRegistry.register(BftsmartConsensusViewSettings.class);
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
    }

    @RequestMapping(path = URL_GET_ACCESS_SPEC, method = RequestMethod.GET)
    @Override
    public AccessSpecification getAccessSpecification() {
        HashDigest[] ledgers = new HashDigest[ledgerPeers.size()];
        String[] consensusProviders = new String[ledgers.length];
        int i = 0;
        for (Entry<HashDigest, NodeServer> ledgerNode : ledgerPeers.entrySet()) {
            ledgers[i] = ledgerNode.getKey();
            consensusProviders[i] = ledgerNode.getValue().getProviderName();
            i++;
        }
        return new AccessSpecification(ledgers, consensusProviders);
    }

    /**
     * 接入认证；
     *
     * @param authRequest
     * @return
     */
    @RequestMapping(path = URL_AUTH_GATEWAY, method = RequestMethod.POST, consumes = BinaryMessageConverter.CONTENT_TYPE_VALUE)
    @Override
    public GatewayAuthResponse authenticateGateway(@RequestBody GatewayAuthRequest authRequest) {
        if (ledgerPeers.size() == 0 || authRequest == null) {
            return null;
        }

        HashDigest[] authLedgers = authRequest.getLedgers();
        ClientCredential[] clientCredentialOfRequests = authRequest.getCredentials();
        if (authLedgers == null || authLedgers.length == 0 || clientCredentialOfRequests == null
                || clientCredentialOfRequests.length == 0) {
            return null;
        }

        GatewayAuthResponse gatewayAuthResponse = new GatewayAuthResponse();
        List<LedgerIncomingSettings> ledgerIncomingList = new ArrayList<LedgerIncomingSettings>();

        int i = -1;
        for (HashDigest ledgerHash : authLedgers) {
            i++;
            NodeServer peer = ledgerPeers.get(ledgerHash);
            if (peer == null) {
                continue;
            }

            String peerProviderName = peer.getProviderName();

            ConsensusProvider provider = ConsensusProviders.getProvider(peer.getProviderName());

            ClientIncomingSettings clientIncomingSettings = null;
            ClientCredential clientRedential = clientCredentialOfRequests[i];
            if (!peerProviderName.equalsIgnoreCase(clientRedential.getProviderName())) {
                // 忽略掉不匹配的“共识客户端提供者程序”认证信息；
                continue;
            }

            // 用户账户校验，必须为非移除状态的共识节点
            LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);
            if (null == ledgerRepo) {
                continue;
            }
            boolean isParticipantNode = false;
            PubKey clientPubKey = clientRedential.getPubKey();
            for (ParticipantNode participantNode : ledgerRepo.getAdminInfo().getParticipants()) {
                if (participantNode.getPubKey().equals(clientPubKey) &&
                        participantNode.getParticipantNodeState() != ParticipantNodeState.DEACTIVATED) {
                    isParticipantNode = true;
                    break;
                }
            }
            if (!isParticipantNode) {
                continue;
            }

            try {
                UserAccount peerAccount = ledgerRepo.getUserAccountSet().getAccount(ledgerCurrNodes.get(ledgerHash).getAddress());
                if (peerAccount.getState() != AccountState.NORMAL) {
                    LOGGER.error(String.format("Authenticate ledger[%s] error ! peer state is [%s]", ledgerHash.toBase58(), peerAccount.getState()));
                    continue;
                }
                UserAccount gwAccount = ledgerRepo.getUserAccountSet().getAccount(AddressEncoding.generateAddress(clientPubKey));
                if (gwAccount.getState() != AccountState.NORMAL) {
                    LOGGER.error(String.format("Authenticate ledger[%s] error ! gateway state is [%s]", ledgerHash.toBase58(), peerAccount.getState()));
                    continue;
                }
                // 证书模式下认证校验
                if (ledgerIdMode.get(ledgerHash) == IdentityMode.CA) {
                    // 当前Peer证书
                    X509Certificate peerCA = CertificateUtils.parseCertificate(peerAccount.getCertificate());
                    CertificateUtils.checkCertificateRole(peerCA, CertificateRole.PEER);
                    CertificateUtils.checkValidity(peerCA);

                    X509Certificate[] ledgerCAs = CertificateUtils.parseCertificates(ledgerRepo.getAdminInfo().getMetadata().getLedgerCertificates());
                    Arrays.stream(ledgerCAs).forEach(issuer -> CertificateUtils.checkCACertificate(issuer));

                    // 当前账本证书中当前节点证书发布者
                    X509Certificate[] peerIssuers = CertificateUtils.findIssuers(peerCA, ledgerCAs);
                    CertificateUtils.checkValidityAny(peerIssuers);

                    // 接入网关CA
                    X509Certificate gwCA = CertificateUtils.parseCertificate(gwAccount.getCertificate());
                    CertificateUtils.checkCertificateRole(gwCA, CertificateRole.GW);
                    CertificateUtils.checkValidity(gwCA);
                    X509Certificate[] gwIssuers = CertificateUtils.findIssuers(gwCA, ledgerCAs);
                    CertificateUtils.checkValidityAny(gwIssuers);
                }
                clientIncomingSettings = peer.getClientAuthencationService().authencateIncoming(clientRedential);
            } catch (Exception e) {
                // 个别账本的认证失败不应该影响其它账本的认证；
                LOGGER.error(String.format("Authenticate ledger[%s] error !", ledgerHash.toBase58()), e);
                continue;
            }

            byte[] clientIncomingBytes = provider.getSettingsFactory().getIncomingSettingsEncoder()
                    .encode(clientIncomingSettings);
            String base64ClientIncomingSettings = ByteArray.toBase64(clientIncomingBytes);

            LedgerIncomingSettings ledgerIncomingSetting = new LedgerIncomingSettings();
            ledgerIncomingSetting.setLedgerHash(ledgerHash);

            // 使用非代理对象，防止JSON序列化异常
            ledgerIncomingSetting.setCryptoSetting(new CryptoConfigInfo(ledgerCryptoSettings.get(ledgerHash)));
            ledgerIncomingSetting.setConsensusClientSettings(base64ClientIncomingSettings);
            ledgerIncomingSetting.setProviderName(peerProviderName);

            ledgerIncomingList.add(ledgerIncomingSetting);

        }
        gatewayAuthResponse
                .setLedgers(ledgerIncomingList.toArray(new LedgerIncomingSettings[ledgerIncomingList.size()]));
        return gatewayAuthResponse;
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
                // 不同的账本实现处理隔离
                try {
                    setConfig(config.getLedger(ledgerHash), ledgerHash);
                } catch (Exception e) {
                    LOGGER.error("Exception occurred on setConfig! Exception ledger = {}, Exception cause = {}", Base58Utils.encode(ledgerHash.toBytes()), e.getMessage());
                    continue;
                }
            }

        } catch (Exception e) {
            LOGGER.error("Peer start exception, Error occurred on configing LedgerBindingConfig! --" + e.getMessage(), e);
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
            ledgerRepository = ledgerManager.register(ledgerHash, dbConnNew.getStorageService(), bindingConfig.getDataStructure());

            ledgerAdminAccount = ledgerRepository.getAdminInfo();

            ConsensusProvider provider = getProvider(ledgerAdminAccount);

            // load consensus setting;
            ConsensusViewSettings csSettings = getConsensusSetting(ledgerAdminAccount);

            // find current node;

            for (ParticipantNode participantNode : ledgerAdminAccount.getParticipants()) {
                LOGGER.debug("[!!!] ParticipantNode.getAddress().toString()= {}", participantNode.getAddress().toString());

                if (participantNode.getAddress().toString().equals(bindingConfig.getParticipant().getAddress())) {
                    currentNode = participantNode;
                    break;
                }
            }
            if (currentNode == null) {
                throw new IllegalArgumentException("Current node is not found from the participant settings of ledger["
                        + ledgerHash.toBase58() + "]!");
            }

            LedgerMetadata_V2 metadata = ledgerRepository.getAdminInfo().getMetadata();
            ledgerIdMode.put(ledgerHash, null != metadata.getIdentityMode() ? metadata.getIdentityMode() : IdentityMode.KEYPAIR);
            if (metadata.getIdentityMode() == IdentityMode.CA) {
                X509Certificate peerCA = CertificateUtils.parseCertificate(ledgerRepository.getUserAccountSet().getAccount(currentNode.getAddress()).getCertificate());
                X509Certificate[] issuers = CertificateUtils.findIssuers(peerCA, CertificateUtils.parseCertificates(metadata.getLedgerCertificates()));
                // 校验根证书
                Arrays.stream(issuers).forEach(issuer -> CertificateUtils.checkCACertificate(issuer));
                CertificateUtils.checkValidityAny(issuers);
                // 校验节点证书
                CertificateUtils.checkCertificateRole(peerCA, CertificateRole.PEER);
                CertificateUtils.checkValidity(peerCA);
            }
            // 处于ACTIVED状态的参与方才会创建共识节点服务
            if (currentNode.getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {
                ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(ledgerHash.toBase58(), csSettings,
                        currentNode.getAddress().toBase58(), bindingConfig.getSslSecurity());
                ((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());
                Storage consensusRuntimeStorage = getConsensusRuntimeStorage(ledgerHash);
                server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
                        consensusStateManager, consensusRuntimeStorage);
                ledgerPeers.put(ledgerHash, server);
            }
            ledgerQuerys.put(ledgerHash, ledgerRepository);
            ledgerCurrNodes.put(ledgerHash, currentNode);
            ledgerCryptoSettings.put(ledgerHash, ledgerAdminAccount.getSettings().getCryptoSetting());
            ledgerKeypairs.put(ledgerHash, loadIdentity(currentNode, bindingConfig));
            ledgerSecurities.put(ledgerHash, bindingConfig.getSslSecurity());
        } catch (Exception e) {
            ledgerManager.unregister(ledgerHash);
            throw e;
        }

        return server;
    }

    /**
     * 返回指定账本的共识运行时存储；
     *
     * @param ledgerHash
     * @return
     */
    private Storage getConsensusRuntimeStorage(HashDigest ledgerHash) {
        return storage.getStorage(ledgerHash.toBase58()).getStorage(STORAGE_CONSENSUS);
    }

    @Override
    public ConsensusRealm[] getRealms() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public void runAllRealms() {
        if (ledgerPeers != null && !ledgerPeers.isEmpty()) {
            // 每个账本使用独立的线程启动
            ThreadPoolExecutor executor = initLedgerLoadExecutor(ledgerPeers.size());
            for (NodeServer peer : ledgerPeers.values()) {
                executor.execute(() -> {
                    runRealm(peer);
                });
            }
        }
    }

    @Override
    public void runRealm(NodeServer nodeServer) {
        nodeServer.start();
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("Destroy ManagementController Bean!");
        closeAllRealms();
    }

    @Override
    public void closeAllRealms() {
        for (NodeServer peer : ledgerPeers.values()) {
            peer.stop();
        }
    }

    @RequestMapping(path = "/monitor/consensus/nodestate/{ledgerHash}", method = RequestMethod.GET)
    public NodeState getConsensusNodeState(@PathVariable("ledgerHash") String base58LedgerHash) {
        byte[] ledgerHashBytes;
        try {
            ledgerHashBytes = Base58Utils.decode(base58LedgerHash);
        } catch (Exception e) {
            String errMsg = "Error occurred while resolving the base58 ledger hash string[" + base58LedgerHash + "]! --"
                    + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
        HashDigest ledgerHash;
        try {
            ledgerHash = Crypto.resolveAsHashDigest(ledgerHashBytes);
        } catch (Exception e) {
            String errMsg = "Error occurred while resolving the ledger hash[" + base58LedgerHash + "]! --"
                    + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
        NodeServer nodeServer = ledgerPeers.get(ledgerHash);
        if (nodeServer == null) {
            throw new BusinessException("The consensus node of ledger[" + base58LedgerHash + "] don't exist!");
        }
        try {
//			String stateInfo = JSONSerializeUtils.serializeToJSON(nodeServer.getState(), true);
            return nodeServer.getState();
        } catch (Exception e) {
            String errMsg = "Error occurred while detecting the state info of the current consensus node in ledger["
                    + base58LedgerHash + "]! --" + e.getMessage();
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

    /**
     * KV类型的账本数据库支持检验账本区块内交易是否被篡改
     *
     * @return
     */
    @RequestMapping(path = "/monitor/ledger/antitamper/{ledgerHash}/{blockHeight}", method = RequestMethod.GET)
    public boolean verifyLedgerTampered(@PathVariable("ledgerHash") String base58LedgerHash, @PathVariable("blockHeight") long blockHeight) {

        HashDigest ledgerHash = Crypto.resolveAsHashDigest(Base58Utils.decode(base58LedgerHash));

        LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);

        if (ledgerKeypairs.get(ledgerHash) == null) {
            LOGGER.info("ledger hash not exist!");
            return false;
        }
        if (ledgerRepo.retrieveLatestBlock().getHeight() < blockHeight || blockHeight < 1) {
            LOGGER.info("blockHeight parameter invalid!");
            return false;
        }

        if (ledgerRepo.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
            LOGGER.info("MERKLE_TREE type ledger database not support anti tamper verify!");
            return false;
        }
        TransactionSet currTransactionSet = ledgerRepo.getTransactionSet(ledgerRepo.getBlock(blockHeight));
        int currentHeightTxTotalNums = (int) ledgerRepo.getTransactionSet(ledgerRepo.getBlock(blockHeight)).getTotalCount();

        TransactionSet lastTransactionSet = null;
        int lastHeightTxTotalNums = 0;

        lastTransactionSet = ledgerRepo.getTransactionSet(ledgerRepo.getBlock(blockHeight - 1));
        lastHeightTxTotalNums = (int) lastTransactionSet.getTotalCount();

        // 取当前高度的增量交易数，在增量交易里进行查找
        int currentHeightTxNums = currentHeightTxTotalNums - lastHeightTxTotalNums;

        ArrayList<HashDigest> transactions = new ArrayList<>();

        for (int i = 0; i < currentHeightTxNums; i++) {
            transactions.add(currTransactionSet.getTransactions(lastHeightTxTotalNums + i, 1)[0].getRequest().getTransactionHash());
        }

        HashDigest computeTxSetRootHash = new MerkleTreeSimple(Crypto.getHashFunction(ledgerRepo.getAdminInfo().getSettings().getCryptoSetting().getHashAlgorithm()), lastTransactionSet.getRootHash(), transactions).root();


        return computeTxSetRootHash.toBase58().equals(currTransactionSet.getRootHash().toBase58());
    }

    /**
     * 输出当前节点状态到日志文件
     *
     * @return
     */
    @RequestMapping(path = "/node/log", method = RequestMethod.GET)
    public void createNodeLog() {
        if (DEFAULT_DIR.length() == 0) {
            try {
                URL resource = ManagementController.class.getResource("/");
                if (resource != null) {
                    String libPath = resource.getPath();
                    if (libPath != null && libPath.length() > 0) {
                        DEFAULT_DIR = libPath;
                        this.logDefaultFile = File.separator + new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss").format(Calendar.getInstance().getTime()) + "-node.log";
                    }
                } else {
                    File libDir = new File(ManagementController.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                    LOGGER.info("ManagementController's lib path = {} !", libDir.getAbsolutePath());
                    DEFAULT_DIR = libDir.getParentFile().getParentFile().getPath();
                    this.logDefaultFile = File.separator + "logs" + File.separator + new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss").format(Calendar.getInstance().getTime()) + "-node.log";
                    LOGGER.debug("logDefaultFile = {}", logDefaultFile);
                }
            } catch (Exception e) {
                LOGGER.error("create node log file error!", e);
            }
        }

        try {
            String logPath = DEFAULT_DIR + logDefaultFile;
            File nodeLogFile = new File(logPath);

            if (!nodeLogFile.exists()) {
                nodeLogFile.createNewFile();
            }

            BufferedWriter out = new BufferedWriter(new FileWriter(nodeLogFile, true));
            writeStateToLog(out);

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeStateToLog(BufferedWriter out) {
        try {
            for (HashDigest ledgerHash : ledgerPeers.keySet()) {

                String base58LedgerHash = Base58Utils.encode(ledgerHash.toBytes());
                NodeServer nodeServer = ledgerPeers.get(ledgerHash);

                if (nodeServer == null) {
                    throw new BusinessException("The consensus node of ledger[" + base58LedgerHash + "] don't exist!");
                }

                BftsmartNodeState nodeState = (BftsmartNodeState) nodeServer.getState();

                out.write("==========================Ledger = " + base58LedgerHash + "=============================\r\n");

                out.write("Time = " + new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss:SSS").format(Calendar.getInstance().getTime()) + "\r\n");

                out.write("###Node State:### \r\n");
                out.write("{Running : " + String.valueOf(nodeState.isRunning()) + ", ");
                out.write("NodeID : " + String.valueOf(nodeState.getNodeID()) + ", ");
                out.write("isLeader : " + String.valueOf(nodeState.isLeader()) + "}\r\n");

                out.write("###View State:### \r\n");
                out.write("{ViewID: " + String.valueOf(nodeState.getViewState().getViewID()) + ", " + "ViewN : " + String.valueOf(nodeState.getViewState().getViewN()) + ", " +
                        "ViewF: " + String.valueOf(nodeState.getViewState().getViewF()) + ", " + "Quorum: " + String.valueOf(nodeState.getViewState().getQuorum()) + "}\r\n");

                out.write("View Procs: {");
                int procCount = 0, procs = nodeState.getViewState().getProcessIDs().length;
                for (int procid : nodeState.getViewState().getProcessIDs()) {
                    out.write(String.valueOf(procid));
                    if (++procCount != procs) {
                        out.write(",");
                    } else {
                        out.write("}\r\n");
                    }
                }
                out.write("View Procs Address: {");
                int addressCount = 0;
                for (NodeNetworkAddress nodeNetworkAddress : nodeState.getViewState().getProcessNetAddresses()) {
                    out.write("(host: " + nodeNetworkAddress.getHost() + ", consensusport: " + String.valueOf(nodeNetworkAddress.getConsensusPort()) + ", monitorport: " + String.valueOf(nodeNetworkAddress.getMonitorPort()) + ")");
                    if (++addressCount != procs) {
                        out.write(",");
                    } else {
                        out.write("}\r\n");
                    }
                }

                out.write("###Consensus State:###\r\n");
                out.write("{currentcid: " + String.valueOf(nodeState.getConsensusState().getConensusID()) + ", lastcid: " + String.valueOf(nodeState.getConsensusState().getLastConensusID()) + ", leaderid: " + String.valueOf(nodeState.getConsensusState().getLeaderID()) + "}\r\n");

                out.write("###Leader State:###\r\n");
                out.write("{leaderid: " + String.valueOf(nodeState.getLeaderState().getLeaderID()) + ", lastregency: " + String.valueOf(nodeState.getLeaderState().getLastRegency()) + ", nxtregency: " + String.valueOf(nodeState.getLeaderState().getNextRegency()) + "}\r\n");

                out.write("###Communication State:###\r\n");
                out.write("{tomlayerRunning: " + String.valueOf(nodeState.getCommunicationState().isTomLayerRunning()) + ", tomThreadAlived: " + String.valueOf(nodeState.getCommunicationState().isTomLayerThreadAlived()) + ", deliverThreadAlived: " + String.valueOf(nodeState.getCommunicationState().isDeliverThreadAlived()) + "}\r\n");
                out.write("\r\n");
            }
        } catch (Exception e) {
            LOGGER.error("write state to node log file error!", e);
        }
    }

    /**
     * 区块同步：
     * 从指定节点同步最新区块信息，调用此接口会执行NodeServer重建
     *
     * @param ledgerHash 账本
     * @param syncHost   同步节点IP
     * @param syncPort   同步节点端口
     * @return
     */
    @RequestMapping(path = "/block/sync", method = RequestMethod.POST)
    public WebResponse syncBlock(@RequestParam("ledgerHash") String ledgerHash,
                                 @RequestParam("syncHost") String syncHost,
                                 @RequestParam("syncPort") int syncPort,
                                 @RequestParam(name = "syncSecure", required = false, defaultValue = "false") boolean syncSecure,
                                 @RequestParam(name = "restart", required = false, defaultValue = "false") boolean restart) {
        try {
            HashDigest ledger = Crypto.resolveAsHashDigest(Base58Utils.decode(ledgerHash));
            if (!ledgerKeypairs.containsKey(ledger)) {
                return WebResponse.createFailureResult(-1, "input ledger hash not exist!");
            }

            LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledger);

            LedgerBlock ledgerLatestBlock = ledgerRepo.retrieveLatestBlock();
            LedgerAdminInfo ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerLatestBlock);

            // 目前仅支持BFT-SMaRt
            if (ledgerAdminInfo.getSettings().getConsensusProvider().equals(ConsensusServiceFactory.BFTSMART_PROVIDER)) {

                // 检查本地节点与远端节点在库上是否存在差异,有差异的进行差异交易重放
                ServiceEndpoint endpoint = new ServiceEndpoint(new NetworkAddress(syncHost, syncPort, syncSecure));
                SSLSecurity sslSecurity = ledgerSecurities.get(ledger);
                endpoint.setSslSecurity(sslSecurity);
                WebResponse webResponse = checkLedgerDiff(ledger, ledgerRepo, ledgerLatestBlock, endpoint);
                if (!webResponse.isSuccess()) {
                    return webResponse;
                }

                // 重建 NodeServer
                if (restart) {
                    setupServer(ledgerRepo, false);
                }

                LOGGER.info("sync block success!");

                return WebResponse.createSuccessResult(null);

            } else {
                return WebResponse.createSuccessResult(null);
            }

        } catch (Exception e) {
            LOGGER.error("sync block failed!", e);
            return WebResponse.createFailureResult(-1, "sync block failed! " + e.getMessage());
        }
    }

    /**
     * 激活参与方； <br>
     * <p>
     * 此方法假设当前节点是一个新建但尚未加入共识网络的共识节点, 通过此方法接收一笔用于实现管理操作的交易；
     *
     * <p>
     * <p>
     * 此方法接收到交易之后，先把交易提交到已有的共识网络执行； <br>
     * <p>
     * 如果交易通过验证并执行成功，则将交易在本地的账本中以本地方式执行; <br>
     * <p>
     * 如果执行之后的新区块一致，则提交本地区块；
     *
     * <p>
     * 如果操作中涉及到共识参与方的共识参数变化，将触发将此节点的共识拓扑改变的操作；
     *
     * @param base58LedgerHash   base58格式的账本哈希；
     * @param consensusHost      激活参与方的共识Ip
     * @param consensusPort      激活参与方的共识Port
     * @param consensusSecure    激活参与方的共识服务是否开启安全连接
     * @param remoteManageHost   提供完备数据库的共识节点管理IP
     * @param remoteManagePort   提供完备数据库的共识节点管理Port
     * @param remoteManageSecure 提供完备数据库的共识节点管理服务是否开启安全连接
     * @return
     */
    @RequestMapping(path = "/delegate/activeparticipant", method = RequestMethod.POST)
    public WebResponse activateParticipant(@RequestParam("ledgerHash") String base58LedgerHash,
                                           @RequestParam("consensusHost") String consensusHost,
                                           @RequestParam("consensusPort") int consensusPort,
                                           @RequestParam(name = "consensusStorage", required = false) String consensusStorage,
                                           @RequestParam(name = "consensusSecure", required = false, defaultValue = "false") boolean consensusSecure,
                                           @RequestParam("remoteManageHost") String remoteManageHost,
                                           @RequestParam("remoteManagePort") int remoteManagePort,
                                           @RequestParam(name = "remoteManageSecure", required = false, defaultValue = "false") boolean remoteManageSecure,
                                           @RequestParam(name = "shutdown", required = false, defaultValue = "false") boolean shutdown) {
        try {
            HashDigest ledgerHash = Crypto.resolveAsHashDigest(Base58Utils.decode(base58LedgerHash));

            if (ledgerKeypairs.get(ledgerHash) == null) {
                return WebResponse.createFailureResult(-1, "ledger hash not exist!");
            }

            LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);
            LedgerAdminInfo ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock());

            String currentProvider = ledgerAdminInfo.getSettings().getConsensusProvider();
            IParticipantManagerService participantService = consensusServiceFactory.getService(currentProvider);
            if (participantService == null || !participantService.supportManagerParticipant()){
                return WebResponse.createFailureResult(-1, "not support operation");
            }

            ParticipantContext context = ParticipantContext.buildContext(ledgerHash,
                    ledgerRepo,
                    ledgerAdminInfo,
                    currentProvider,
                    participantService,
                    ledgerSecurities.get(ledgerHash));
            context.setProperty(IParticipantManagerService.RAFT_CONSENSUS_NODE_STORAGE , consensusStorage);
            context.setProperty(ParticipantContext.HASH_ALG_PROP, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());
            context.setProperty(ParticipantContext.ENDPOINT_SIGNER_PROP, new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
                    ledgerKeypairs.get(ledgerHash).getPrivKey()));

            // 检查节点信息
            ParticipantNode addNode = getCurrentNode(ledgerAdminInfo, ledgerCurrNodes.get(ledgerHash).getAddress().toString());
            NodeSettings nodeSettings = getConsensusNodeSettings(ledgerQuerys.values(), consensusHost, consensusPort);
            if (nodeSettings != null) {
                if (!BytesUtils.equals(addNode.getPubKey().toBytes(), nodeSettings.getPubKey().toBytes())) {
                    return WebResponse.createFailureResult(-1, String.format("%s:%d already occupied!", consensusHost, consensusPort, addNode.getAddress().toBase58()));
                } else {
                    LOGGER.info("participant {}:{} exists and status is CONSENSUS!", consensusHost, consensusPort);
                    // 节点存在且状态为激活，返回成功
                    return WebResponse.createSuccessResult(null);
                }
            }

            if (addNode.getParticipantNodeState() == ParticipantNodeState.CONSENSUS) {
                LOGGER.info("participant {}:{} already in CONSENSUS state!!", consensusHost, consensusPort);
                return WebResponse.createSuccessResult("participant already in CONSENSUS state!");
            }

            NetworkAddress addConsensusNodeAddress = new NetworkAddress(consensusHost, consensusPort, consensusSecure);
            ServiceEndpoint remoteEndpoint = new ServiceEndpoint(new NetworkAddress(remoteManageHost, remoteManagePort, remoteManageSecure));
            remoteEndpoint.setSslSecurity(ledgerSecurities.get(ledgerHash));

            LOGGER.info("active participant {}:{}!", consensusHost, consensusPort);
            return activeParticipant(context, addNode, addConsensusNodeAddress, shutdown, remoteEndpoint);
        } catch (Exception e) {
            LOGGER.error(String.format("activate participant %s:%d failed!", consensusHost, consensusPort), e);
            return WebResponse.createFailureResult(-1, "activate participant failed! " + e.getMessage());
        }finally {
            ParticipantContext.clear();
        }
    }

    /**
     * 更新参与方共识配置（IP，端口，是否开启安全连接）； <br>
     *
     * @param base58LedgerHash base58格式的账本哈希；
     * @param consensusHost    待更新参与方的共识Ip
     * @param consensusPort    待更新参与方的共识Port
     * @param consensusSecure  待更新参与方的共识服务是否开启安全连接
     * @return
     */
    @RequestMapping(path = "/delegate/updateparticipant", method = RequestMethod.POST)
    public WebResponse updateParticipant(@RequestParam("ledgerHash") String base58LedgerHash,
                                         @RequestParam("consensusHost") String consensusHost,
                                         @RequestParam("consensusPort") int consensusPort,
                                         @RequestParam(name = "consensusStorage", required = false) String consensusStorage,
                                         @RequestParam(name = "consensusSecure", required = false, defaultValue = "false") boolean consensusSecure,
                                         @RequestParam(name = "shutdown", required = false, defaultValue = "false") boolean shutdown) {
        try {
            HashDigest ledgerHash = Crypto.resolveAsHashDigest(Base58Utils.decode(base58LedgerHash));

            if (ledgerKeypairs.get(ledgerHash) == null) {
                return WebResponse.createFailureResult(-1, "ledger hash not exist!");
            }

            LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);
            LedgerAdminInfo ledgerAdminInfo = ledgerRepo.getAdminInfo(ledgerRepo.retrieveLatestBlock());

            String currentProvider = ledgerAdminInfo.getSettings().getConsensusProvider();
            IParticipantManagerService participantService = consensusServiceFactory.getService(currentProvider);
            if (participantService == null || !participantService.supportManagerParticipant()){
                return WebResponse.createFailureResult(-1, "not support operation");
            }

            ParticipantContext context = ParticipantContext.buildContext(ledgerHash,
                    ledgerRepo,
                    ledgerAdminInfo,
                    currentProvider,
                    participantService,
                    ledgerSecurities.get(ledgerHash));
            context.setProperty(IParticipantManagerService.RAFT_CONSENSUS_NODE_STORAGE , consensusStorage);
            context.setProperty(ParticipantContext.HASH_ALG_PROP, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());
            context.setProperty(ParticipantContext.ENDPOINT_SIGNER_PROP, new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
                    ledgerKeypairs.get(ledgerHash).getPrivKey()));

            // 检查节点信息
            ParticipantNode updateNode = getCurrentNode(ledgerAdminInfo, ledgerCurrNodes.get(ledgerHash).getAddress().toString());
            NodeSettings nodeSettings = getConsensusNodeSettings(ledgerQuerys.values(), consensusHost, consensusPort);
            if (nodeSettings != null) {
                if (!BytesUtils.equals(updateNode.getPubKey().toBytes(), nodeSettings.getPubKey().toBytes())) {
                    return WebResponse.createFailureResult(-1, String.format("%s:%d already occupied!", consensusHost, consensusPort, updateNode.getAddress().toBase58()));
                } else {
                    LOGGER.info("participant {}:{} exists and status is CONSENSUS!", consensusHost, consensusPort);
                    // 节点存在且状态为激活，返回成功
                    return WebResponse.createSuccessResult(null);
                }
            }

            if (updateNode.getParticipantNodeState() != ParticipantNodeState.CONSENSUS) {
                return WebResponse.createFailureResult(-1, "participant not in CONSENSUS state!");
            }

            NetworkAddress updateConsensusNodeAddress = new NetworkAddress(consensusHost, consensusPort, consensusSecure);

            LOGGER.info("update participant {}:{}", consensusHost, consensusPort);
            return updateParticipant(context, updateNode, updateConsensusNodeAddress, shutdown);
        } catch (Exception e) {
            LOGGER.error(String.format("update participant %s:%d failed!", consensusHost, consensusPort), e);
            return WebResponse.createFailureResult(-1, "update participant failed! " + e.getMessage());
        }finally {
            ParticipantContext.clear();
        }
    }


    private WebResponse activeParticipant(ParticipantContext context,
                                          ParticipantNode node,
                                          NetworkAddress addConsensusNodeAddress,
                                          boolean shutdown,
                                          ServiceEndpoint remoteEndpoint) {

        HashDigest ledgerHash = context.ledgerHash();
        LedgerRepository ledgerRepo = context.ledgerRepo();
        IParticipantManagerService participantService = context.participantService();

        Properties customProperties = participantService.getCustomProperties(context);
        // 由本节点准备交易
        TransactionRequest txRequest = prepareActiveTx(ledgerHash, node, addConsensusNodeAddress, customProperties);
        // 为交易添加本节点的签名信息，防止无法通过安全策略检查
        txRequest = addNodeSigner(txRequest);

        List<NodeSettings> origConsensusNodes = SearchOtherOrigConsensusNodes(ledgerRepo, node);
        // 连接原有的共识网络,把交易提交到目标账本的原有共识网络进行共识，即在原有共识网络中执行新参与方的状态激活操作
        TransactionResponse remoteTxResponse = participantService.submitNodeStateChangeTx(context, txRequest, origConsensusNodes);

        if (remoteTxResponse.isSuccess() && replayTransaction(ledgerRepo, node, remoteEndpoint)) {
            try {

                if(participantService.startServerBeforeApplyNodeChange()){
                    setupServer(ledgerRepo, false);
                }

                //使用共识原语变更节点
                WebResponse webResponse = participantService.applyConsensusGroupNodeChange(context, ledgerCurrNodes.get(ledgerHash), addConsensusNodeAddress, origConsensusNodes, ParticipantUpdateType.ACTIVE);
                if(!webResponse.isSuccess()){
                    return webResponse;
                }

                if(!participantService.startServerBeforeApplyNodeChange()){
                    setupServer(ledgerRepo, shutdown);
                }

                return webResponse;
            } catch (Exception e) {
                return WebResponse.createFailureResult(-1, "commit tx to orig consensus, tx execute succ but view update failed, please restart all nodes and copy database for new participant node!");
            }
        }

        return WebResponse.createFailureResult(remoteTxResponse.getExecutionState().CODE, remoteTxResponse.getExecutionState().toString());
    }

    private WebResponse updateParticipant(ParticipantContext context,
                                          ParticipantNode node,
                                          NetworkAddress updateConsensusNodeAddress,
                                          boolean shutdown) {

        HashDigest ledgerHash = context.ledgerHash();
        LedgerRepository ledgerRepo = context.ledgerRepo();
        IParticipantManagerService participantService = context.participantService();

        Properties customProperties = participantService.getCustomProperties(context);
        // 由本节点准备交易
        TransactionRequest txRequest = prepareUpdateTx(ledgerHash, node, updateConsensusNodeAddress, customProperties);

        // 为交易添加本节点的签名信息，防止无法通过安全策略检查
        txRequest = addNodeSigner(txRequest);

        List<NodeSettings> origConsensusNodes = SearchOrigConsensusNodes(ledgerRepo);

        // 连接原有的共识网络,把交易提交到目标账本的原有共识网络进行共识，即在原有共识网络中执行新参与方的状态激活操作
        TransactionResponse remoteTxResponse = participantService.submitNodeStateChangeTx(context, txRequest, origConsensusNodes);

        if(!remoteTxResponse.isSuccess()){
            return WebResponse.createFailureResult(-1,
                    "commit tx to orig consensus, tx execute failed, please retry activate participant!");
        }

        // 保证原有共识网络账本状态与共识协议的视图更新信息一致
        try{

            waitUtilReachHeight(remoteTxResponse.getBlockHeight(), ledgerRepo);

            if(participantService.startServerBeforeApplyNodeChange()){
                setupServer(ledgerRepo, false);
            }

            WebResponse webResponse = participantService.applyConsensusGroupNodeChange(context,
                    ledgerCurrNodes.get(ledgerHash),
                    updateConsensusNodeAddress,
                    origConsensusNodes,
                    ParticipantUpdateType.UPDATE);

            if(!webResponse.isSuccess()){
                return webResponse;
            }
        } catch (Exception e) {
            LOGGER.error("updateView exception!", e);
            return WebResponse.createFailureResult(-1,
                    "commit tx to orig consensus, tx execute succ but view update failed, please restart all nodes and copy database for new participant node!");
        }

        if(!participantService.startServerBeforeApplyNodeChange()){
            setupServer(ledgerRepo, shutdown);
        }

        return WebResponse.createSuccessResult(null);
    }

    private void waitUtilReachHeight(long blockHeight, LedgerRepository ledgerRepo) {
        for(;;){
            LedgerBlock ledgerBlock = ledgerRepo.retrieveLatestBlock();
            if(ledgerBlock.getHeight() >= blockHeight){
                return;
            }
            Thread.yield();
        }
    }

    /**
     * 根据IP和端口获取处于激活状态的节点NodeSettings，不存在返回null
     *
     * @param ledgers       账本数据库
     * @param consensusHost 节点IP
     * @param consensusPort 节点端口
     * @return
     */
    private NodeSettings getConsensusNodeSettings(Collection<LedgerQuery> ledgers, String consensusHost, int consensusPort) {

        for (LedgerQuery ledgerRepo : ledgers) {
            for (NodeSettings nodeSettings : SearchOrigConsensusNodes((LedgerRepository) ledgerRepo)) {
                NetworkAddress netWorkAddress = consensusServiceFactory.getNetWorkAddress(nodeSettings);
                if(netWorkAddress == null){
                    continue;
                }
                String host = netWorkAddress.getHost();
                int port = netWorkAddress.getPort();

                if ((host.equals(consensusHost)) && port == consensusPort) {
                    return nodeSettings;
                }
            }
        }

        return null;
    }

    private void cancelBlock(long blockGenerateTime, TransactionBatchProcessor txBatchProcessor) {
        LedgerEditor.TIMESTAMP_HOLDER.set(blockGenerateTime);
        TransactionBatchResultHandle handle = txBatchProcessor.prepare();
        handle.cancel(LEDGER_ERROR);
    }

    /**
     * 代理交易； <br>
     * <p>
     * 此方法假设当前节点是一个待移除的共识节点, 通过此方法接收一笔用于实现管理操作的交易；
     *
     * <p>
     * <p>
     * 此方法接收到交易之后，先把交易提交到已有的共识网络执行，这个已有网络包括本节点； <br>
     *
     * <p>
     * 如果操作中涉及到共识参与方的共识参数变化，将触发将此节点的共识拓扑改变的操作；
     *
     * @param base58LedgerHash   base58格式的账本哈希；
     * @param participantAddress 待移除参与方的地址
     * @return
     */
    @RequestMapping(path = "/delegate/deactiveparticipant", method = RequestMethod.POST)
    public WebResponse deActivateParticipant(@RequestParam("ledgerHash") String base58LedgerHash,
                                             @RequestParam("participantAddress") String participantAddress) {
        try {
            HashDigest ledgerHash = Crypto.resolveAsHashDigest(Base58Utils.decode(base58LedgerHash));

            // 进行一系列安全检查
            if (ledgerQuerys.get(ledgerHash) == null) {
                return WebResponse.createFailureResult(-1, "input ledgerhash not exist!");
            }

            if (!ledgerCurrNodes.get(ledgerHash).getAddress().toBase58().equals(participantAddress)) {
                return WebResponse.createFailureResult(-1, "deactive participant not me!");
            }

            LedgerRepository ledgerRepo = (LedgerRepository) ledgerQuerys.get(ledgerHash);
            LedgerAdminInfo ledgerAdminInfo = ledgerRepo.getAdminInfo();

            String currentProvider = ledgerAdminInfo.getSettings().getConsensusProvider();
            IParticipantManagerService participantService = consensusServiceFactory.getService(currentProvider);
            if (participantService == null || !participantService.supportManagerParticipant()){
                return WebResponse.createFailureResult(-1, "not support operation");
            }

            // 已经是DEACTIVATED状态
            ParticipantNode node = getCurrentNode(ledgerAdminInfo, participantAddress);
            if (node.getParticipantNodeState() == ParticipantNodeState.DEACTIVATED) {
                return WebResponse.createSuccessResult(null);
            }

            // 已经处于最小节点数环境的共识网络，不能再执行去激活操作
            List<NodeSettings> origConsensusNodes = SearchOrigConsensusNodes(ledgerRepo);
            if (origConsensusNodes.size() <= participantService.minConsensusNodes()) {
                return WebResponse.createFailureResult(-1, "in minimum number of nodes scenario, deactive op is not allowed!");
            }

            ParticipantContext context = ParticipantContext.buildContext(ledgerHash,
                    ledgerRepo,
                    ledgerAdminInfo,
                    currentProvider,
                    participantService,
                    ledgerSecurities.get(ledgerHash));
            context.setProperty(ParticipantContext.HASH_ALG_PROP, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());
            context.setProperty(ParticipantContext.ENDPOINT_SIGNER_PROP, new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
                    ledgerKeypairs.get(ledgerHash).getPrivKey()));
            
            Properties customProperties = participantService.getCustomProperties(context);

            // 由本节点准备交易
            TransactionRequest txRequest = prepareDeActiveTx(ledgerHash, node, customProperties);
            // 为交易添加本节点的签名信息，防止无法通过安全策略检查
            txRequest = addNodeSigner(txRequest);

            // 连接原有的共识网络,把交易提交到目标账本的原有共识网络进行共识，即在原有共识网络中执行参与方的去激活操作，这个原有网络包括本节点
            TransactionResponse txResponse = participantService.submitNodeStateChangeTx(context, txRequest, origConsensusNodes);
            if(!txResponse.isSuccess()){
                return WebResponse.createFailureResult(-1, "commit tx to orig consensus, tx execute failed, please retry deactivate participant!");
            }

            // 保证原有共识网络账本状态与共识协议的视图更新信息一致
            WebResponse response = participantService.applyConsensusGroupNodeChange(context,
                    ledgerCurrNodes.get(ledgerHash),
                    null,
                    origConsensusNodes,
                    ParticipantUpdateType.DEACTIVE
                    );
            ledgerPeers.get(ledgerHash).stop();
            LOGGER.info("updateView success!");
            return response;

        } catch (Exception e) {
            return WebResponse.createFailureResult(-1, "deactivate participant failed!" + e);
        }finally {
            ParticipantContext.clear();
        }
    }

    private ParticipantNode getCurrentNode(LedgerAdminInfo ledgerAdminInfo, String participantAddress) {
        for (ParticipantNode participantNode : ledgerAdminInfo.getParticipants()) {
            if (participantNode.getAddress().toString().equals(participantAddress)) {
                return participantNode;
            }
        }

        throw new IllegalStateException("participant [" + participantAddress + "] not exists");
    }

    private TransactionRequest prepareDeActiveTx(HashDigest ledgerHash, ParticipantNode node,
                                                 Properties customProperties) {

        int deActiveID = node.getId();

        // organize system config properties
        Property[] properties = ParticipantContext.context().participantService()
                .createDeactiveProperties(node.getPubKey(), deActiveID, customProperties);

        TxBuilder txbuilder = new TxBuilder(ledgerHash, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());

        // This transaction contains participant state update and settings update two
        // ops
        txbuilder.states().update(new BlockchainIdentityData(node.getPubKey()), ParticipantNodeState.DEACTIVATED);

        txbuilder.settings().update(properties);

        TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

        reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
                ledgerKeypairs.get(ledgerHash).getPrivKey()));

        return reqBuilder.buildRequest();

    }

    private WebResponse checkLedgerDiff(HashDigest ledgerHash, LedgerRepository ledgerRepository, LedgerBlock ledgerLatestBlock, ServiceEndpoint endpoint) {
        LOGGER.info("check ledger diff from {}:{}:{}", endpoint.getHost(), endpoint.getPort(), endpoint.isSecure());
        long localLatestBlockHeight = ledgerLatestBlock.getHeight();

        HashDigest localLatestBlockHash = ledgerLatestBlock.getHash();

        TransactionBatchResultHandle handle = null;

        try (ServiceConnection httpConnection = ServiceConnectionManager.connect(endpoint)) {

            HttpBlockchainBrowserService queryService = HttpServiceAgent.createService(HttpBlockchainBrowserService.class, httpConnection, null);

            // 激活新节点时，远端管理节点最新区块高度
            long remoteLatestBlockHeight = queryService.getLedger(ledgerHash)
                    .getLatestBlockHeight();

            if ((localLatestBlockHeight <= remoteLatestBlockHeight)) {
                // 检查本节点与拉取节点相同高度的区块，哈希是否一致,不一致说明其中一个节点的数据库被污染了
                HashDigest remoteBlockHash = queryService.getBlock(ledgerHash, localLatestBlockHeight).getHash();

                if (!(localLatestBlockHash.toBase58().equals(remoteBlockHash.toBase58()))) {
                    throw new IllegalStateException(
                            "checkLedgerDiff, ledger database is inconsistent, please check ledger database!");
                }
                // 本节点与拉取节点高度一致，不需要进行交易重放
                if (localLatestBlockHeight == remoteLatestBlockHeight) {
                    return WebResponse.createSuccessResult(null);
                }
            } else {
                throw new IllegalStateException(
                        "checkLedgerDiff, local latest block height > remote node latest block height!");
            }

            OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
            // 对差异进行交易重放
            for (int height = (int) localLatestBlockHeight + 1; height <= remoteLatestBlockHeight; height++) {
                TransactionBatchProcessor txbatchProcessor = new TransactionBatchProcessor(ledgerRepository, opReg);
                // transactions replay
                try {
                    HashDigest pullBlockHash = queryService.getBlock(ledgerHash, height).getHash();
                    long pullBlockTime = queryService.getBlock(ledgerHash, height).getTimestamp();

                    // 获取区块内的增量交易
                    List<LedgerTransaction> addition_transactions = getAdditionalTransactions(queryService, ledgerHash, height);

                    try {
                        for (LedgerTransaction ledgerTransaction : addition_transactions) {
                            txbatchProcessor.schedule(ledgerTransaction.getRequest());
                        }
                    } catch (BlockRollbackException e) {
                        txbatchProcessor.cancel(LEDGER_ERROR);
                        continue;
                    }

                    LedgerEditor.TIMESTAMP_HOLDER.set(pullBlockTime);
                    handle = txbatchProcessor.prepare();

                    if (!(handle.getBlock().getHash().toBase58().equals(pullBlockHash.toBase58()))) {
                        LOGGER.error(
                                "checkLedgerDiff, transactions replay result is inconsistent at height {}",
                                height);
                        throw new IllegalStateException(
                                "checkLedgerDiff, transactions replay, block hash result is inconsistent!");
                    }

                    handle.commit();

                } catch (Exception e) {
                    handle.cancel(LEDGER_ERROR);
                    throw new IllegalStateException(
                            "checkLedgerDiff, transactions replay failed!", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("checkLedgerDiff error!", e);
            return WebResponse.createFailureResult(-1, "checkLedgerDiff error!" + e);
        }

        return WebResponse.createSuccessResult(null);
    }

    private boolean replayTransaction(LedgerRepository ledgerRepository, ParticipantNode node, ServiceEndpoint endpoint) {
        long height = ledgerRepository.getLatestBlock().getHeight();
        HashDigest ledgerHash = ledgerRepository.getLatestBlock().getLedgerHash();
        TransactionBatchResultHandle handle = null;
        OperationHandleRegisteration opReg = new DefaultOperationHandleRegisteration();
        try (ServiceConnection httpConnection = ServiceConnectionManager.connect(endpoint)) {
            HttpBlockchainBrowserService queryService = HttpServiceAgent.createService(HttpBlockchainBrowserService.class, httpConnection, null);
            while (true) {
                boolean getout = false;
                TransactionBatchProcessor batchProcessor = new TransactionBatchProcessor(ledgerRepository, opReg);
                try {
                    height++;
                    LedgerBlock block = queryService.getBlock(ledgerHash, height);
                    // 获取区块内的增量交易
                    List<LedgerTransaction> transactions = getAdditionalTransactions(queryService, ledgerHash, (int) height);
                    try {
                        for (LedgerTransaction ledgerTransaction : transactions) {
                            batchProcessor.schedule(ledgerTransaction.getRequest());
                            Operation[] operations = ledgerTransaction.getRequest().getTransactionContent().getOperations();
                            for (Operation op : operations) {
                                if (op instanceof ParticipantStateUpdateOperation) {
                                    ParticipantStateUpdateOperation psop = (ParticipantStateUpdateOperation) op;
                                    if (psop.getParticipantID().getPubKey().equals(node.getPubKey())) {
                                        getout = true;
                                    }
                                }
                            }
                        }
                    } catch (BlockRollbackException e) {
                        batchProcessor.cancel(LEDGER_ERROR);
                        continue;
                    }

                    LedgerEditor.TIMESTAMP_HOLDER.set(block.getTimestamp());
                    handle = batchProcessor.prepare();

                    if (!(handle.getBlock().getHash().toBase58().equals(block.getHash().toBase58()))) {
                        LOGGER.error("replayTransaction, transactions replay result is inconsistent at height {}", height);
                        throw new IllegalStateException("checkLedgerDiff, transactions replay, block hash result is inconsistent!");
                    }
                    handle.commit();
                    LOGGER.debug("replayTransaction, transactions replay result is consistent at height {}", height);
                    if (getout) {
                        return true;
                    }
                } catch (Exception e) {
                    handle.cancel(LEDGER_ERROR);
                    throw new IllegalStateException("replayTransaction, transactions replay failed!", e);
                }
            }
        }
    }

    private List<LedgerTransaction> getAdditionalTransactions(HttpBlockchainBrowserService queryService, HashDigest ledgerHash, int height) {
        List<LedgerTransaction> txs = new ArrayList<>();
        int fromIndex = 0;

        while (true) {
            try {
                LedgerTransactions transactions = queryService.getAdditionalTransactionsInBinary(ledgerHash, height, fromIndex, 100);
                if (null != transactions && null != transactions.getLedgerTransactions()) {
                    LedgerTransaction[] ts = transactions.getLedgerTransactions();
                    fromIndex += ts.length;
                    for (LedgerTransaction tx : ts) {
                        txs.add(tx);
                    }
                    if (ts.length < 100) {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("get transactions from remote error", e);
                throw new IllegalStateException("get transactions from remote error!", e);
            }
        }
        return txs;

    }

    // 在指定的账本上准备一笔激活参与方状态及系统配置参数的操作
    private TransactionRequest prepareActiveTx(HashDigest ledgerHash, ParticipantNode node,
                                               NetworkAddress addConsensusNodeAddress, Properties customProperties) {

        int activeID = node.getId();

        // organize system config properties
        Property[] properties = ParticipantContext.context().participantService()
                .createActiveProperties(addConsensusNodeAddress, node.getPubKey(), activeID, customProperties);

        TxBuilder txbuilder = new TxBuilder(ledgerHash, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());

        // This transaction contains participant state update and settings update two
        // ops
        txbuilder.states().update(new BlockchainIdentityData(node.getPubKey()), ParticipantNodeState.CONSENSUS);

        txbuilder.settings().update(properties);

        TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

        reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
                ledgerKeypairs.get(ledgerHash).getPrivKey()));

        return reqBuilder.buildRequest();

    }

    // 在指定的账本上准备一笔激活参与方状态及系统配置参数的操作
    private TransactionRequest prepareUpdateTx(HashDigest ledgerHash, ParticipantNode node,
                                               NetworkAddress updateConsensusNodeAddress, Properties customProperties) {

        int activeID = node.getId();

        // organize system config properties
        Property[] properties = ParticipantContext.context().participantService()
                .createUpdateProperties(updateConsensusNodeAddress, node.getPubKey(), activeID, customProperties);

        TxBuilder txbuilder = new TxBuilder(ledgerHash, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());

        // This transaction contains participant state update and settings update two
        // ops
        txbuilder.states().update(new BlockchainIdentityData(node.getPubKey()), ParticipantNodeState.CONSENSUS);

        txbuilder.settings().update(properties);

        TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();

        reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
                ledgerKeypairs.get(ledgerHash).getPrivKey()));

        return reqBuilder.buildRequest();

    }

//    // 在指定的账本上准备一笔reconfig操作交易
//    private TransactionRequest prepareReconfigTx(HashDigest ledgerHash) {
//
//        TxBuilder txbuilder = new TxBuilder(ledgerHash, ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm());
//
//        // This transaction contains one reconfig op
//        txbuilder.reconfigs().record();
//
//        TransactionRequestBuilder reqBuilder = txbuilder.prepareRequest();
//
//        reqBuilder.signAsEndpoint(new AsymmetricKeypair(ledgerKeypairs.get(ledgerHash).getPubKey(),
//                ledgerKeypairs.get(ledgerHash).getPrivKey()));
//
//        return reqBuilder.buildRequest();
//
//    }

    // 加载本参与方的公私钥对身份信息
    private AsymmetricKeypair loadIdentity(ParticipantNode currentNode, BindingConfig bindingConfig) {

        PubKey pubKey = currentNode.getPubKey();

        PrivKey privKey = null;
        String pk = bindingConfig.getParticipant().getPk();
        String pwd = bindingConfig.getParticipant().getPassword();
        String pkPath = bindingConfig.getParticipant().getPkPath();
        if (!StringUtils.isEmpty(pk) && !StringUtils.isEmpty(pwd)) {
            try {
                privKey = KeyGenUtils.decodePrivKey(pk, pwd);
            } catch (Exception e) {
            }
        } else if (!StringUtils.isEmpty(pkPath)) {
            if (!StringUtils.isEmpty(pwd)) {
                try {
                    privKey = KeyGenUtils.decodePrivKey(pk, pwd);
                } catch (Exception e) {
                }
                if (null == privKey) {
                    try {
                        privKey = CertificateUtils.parsePrivKey(pubKey.getAlgorithm(), new File(pkPath), pwd);
                    } catch (Exception e) {
                    }
                }
            } else {
                try {
                    privKey = CertificateUtils.parsePrivKey(pubKey.getAlgorithm(), new File(pkPath));
                } catch (Exception e) {
                }
            }
        }
        if (null == privKey) {
            LOGGER.error("Error keypair or certificate configurations in ledger-binding.conf, participant node: {}", currentNode.getAddress());
        }
        return new AsymmetricKeypair(pubKey, privKey);

    }

    // 视图更新完成，启动共识节点
    private void setupServer(LedgerRepository ledgerRepository, boolean shutdown) {
        try {
            HashDigest ledgerHash = ledgerRepository.getHash();

            // 关闭旧的server
            NodeServer server = ledgerPeers.get(ledgerHash);
            if (null != server) {
                LOGGER.info("stop old server");
                server.stop();
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                LOGGER.error("sleep InterruptedException", e);
            }

            if (shutdown) {
                LOGGER.info("shutdown server in this ip");
                return;
            }

            ParticipantNode currentNode = ledgerCurrNodes.get(ledgerHash);

            LedgerAdminInfo ledgerAdminAccount = ledgerRepository
                    .getAdminInfo(ledgerRepository.getBlock(ledgerRepository.retrieveLatestBlockHeight()));

            // load provider;
            ConsensusProvider provider = getProvider(ledgerAdminAccount);

            // load consensus setting;
            ConsensusViewSettings csSettings = getConsensusSetting(ledgerAdminAccount);

            ServerSettings serverSettings = provider.getServerFactory().buildServerSettings(
                    ledgerHash.toBase58(), csSettings, currentNode.getAddress().toBase58(), ledgerSecurities.get(ledgerHash));

            ((LedgerStateManager) consensusStateManager).setLatestStateId(ledgerRepository.retrieveLatestBlockHeight());

            Storage consensusRuntimeStorage = getConsensusRuntimeStorage(ledgerHash);
            server = provider.getServerFactory().setupServer(serverSettings, consensusMessageHandler,
                    consensusStateManager, consensusRuntimeStorage);

            ledgerPeers.put(ledgerHash, server);

            runRealm(server);

            LOGGER.info("setupServer success!");
        } catch (Exception e) {
            throw new StartServerException("start server fail exception", e);
        }

    }

//    // 通知原有的共识网络更新共识的视图ID
//    private View updateView(LedgerRepository ledgerRepository, NetworkAddress networkAddress, SSLSecurity security,
//                            ParticipantUpdateType participantUpdateType, Properties systemConfig, int viewId, List<NodeSettings> origConsensusNodes) {
//        ParticipantNode currNode = ledgerCurrNodes.get(ledgerRepository.getHash());
//
//        LOGGER.info("ManagementController start updateView operation!");
//
//        try {
//            ServiceProxy peerProxy = createPeerProxy(systemConfig, viewId, origConsensusNodes, security);
//
//            Reconfiguration reconfiguration = new Reconfiguration(peerProxy.getProcessId(), peerProxy);
//
//            if (participantUpdateType == ParticipantUpdateType.ACTIVE) {
//                // addServer的第一个参数指待加入共识的新参与方的编号
//                reconfiguration.addServer(currNode.getId(), networkAddress);
//            } else if (participantUpdateType == ParticipantUpdateType.DEACTIVE) {
//                // 参数为待移除共识节点的id
//                reconfiguration.removeServer(currNode.getId());
//            } else if (participantUpdateType == ParticipantUpdateType.UPDATE) {
//                // 共识参数修改，先移除后添加
//                reconfiguration.removeServer(currNode.getId());
//                reconfiguration.addServer(currNode.getId(), networkAddress);
//            } else {
//                throw new IllegalArgumentException("op type error!");
//            }
//
//            // 把交易作为reconfig操作的扩展信息携带，目的是为了让该操作上链，便于后续跟踪；
//            reconfiguration.addExtendInfo(BinaryProtocol.encode(prepareReconfigTx(ledgerRepository.getHash()), TransactionRequest.class));
//
//            // 执行更新目标共识网络的视图ID
//            ReconfigureReply reconfigureReply = reconfiguration.execute();
//
//            peerProxy.close();
//
//            // 返回新视图
//            return reconfigureReply.getView();
//
//        } catch (Exception e) {
//            throw new ViewUpdateException("view update fail exception!", e);
//        }
//    }

    private TransactionRequest addNodeSigner(TransactionRequest txRequest) {
        TxRequestMessage txMessage = new TxRequestMessage(txRequest);

        HashDigest ledgerHash = txRequest.getTransactionContent().getLedgerHash();
        AsymmetricKeypair peerKeypair = ledgerKeypairs.get(ledgerHash);
        DigitalSignature nodeSigner = SignatureUtils.sign(ledgerCryptoSettings.get(ledgerHash).getHashAlgorithm(),
                txRequest.getTransactionContent(), peerKeypair);

        txMessage.addNodeSignatures(nodeSigner);

        // 计算交易哈希；
//		byte[] nodeRequestBytes = BinaryProtocol.encode(txMessage, TransactionRequest.class);
//		HashFunction hashFunc = Crypto.getHashFunction(cryptoSetting.getHashAlgorithm());
//		HashDigest txHash = hashFunc.hash(nodeRequestBytes);
//		txMessage.setTransactionHash(txHash);

        return txMessage;
    }


    private ConsensusProvider getProvider(LedgerAdminInfo ledgerAdminInfo) {
        // load provider;
        String consensusProvider = ledgerAdminInfo.getSettings().getConsensusProvider();
        ConsensusProvider provider = ConsensusProviders.getProvider(consensusProvider);

        return provider;

    }

    private ConsensusViewSettings getConsensusSetting(LedgerAdminInfo ledgerAdminInfo) {

        ConsensusProvider provider = getProvider(ledgerAdminInfo);

        // load consensus setting
        Bytes csSettingBytes = ledgerAdminInfo.getSettings().getConsensusSetting();
        ConsensusViewSettings csSettings = provider.getSettingsFactory().getConsensusSettingsEncoder()
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

    // 查找原有共识网络中的其他共识节点信息
    private List<NodeSettings> SearchOtherOrigConsensusNodes(LedgerRepository ledgerRepository, ParticipantNode currentNode) {
        List<NodeSettings> origConsensusNodes = new ArrayList<>();
        LedgerAdminInfo ledgerAdminInfo = ledgerRepository.getAdminInfo(ledgerRepository.retrieveLatestBlock());
        // load consensus setting
        ConsensusViewSettings csSettings = getConsensusSetting(ledgerAdminInfo);

        NodeSettings[] nodeSettingsArray = csSettings.getNodes();
        for (NodeSettings nodeSettings : nodeSettingsArray) {
            // 排除不处于激活状态的共识节点
            if (getParticipantState(nodeSettings.getAddress(), ledgerAdminInfo) != ParticipantNodeState.CONSENSUS) {
                continue;
            }

            // 排除当前节点
            if (currentNode.getAddress().toBase58().equals(nodeSettings.getAddress())) {
                continue;
            }

            origConsensusNodes.add(nodeSettings);
        }
        return origConsensusNodes;
    }

    // 查找原有共识网络中的共识节点信息
    private List<NodeSettings> SearchOrigConsensusNodes(LedgerRepository ledgerRepository) {
        List<NodeSettings> origConsensusNodes = new ArrayList<>();
        LedgerAdminInfo ledgerAdminInfo = ledgerRepository.getAdminInfo(ledgerRepository.retrieveLatestBlock());
        // load consensus setting
        ConsensusViewSettings csSettings = getConsensusSetting(ledgerAdminInfo);

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

    private static ThreadPoolExecutor initLedgerLoadExecutor(int coreSize) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("consensus-server-%d").build();

        return new ThreadPoolExecutor(coreSize, coreSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024),
                threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    // 节点更新类型
    public enum ParticipantUpdateType {
        // 激活
        ACTIVE,
        // 移除
        DEACTIVE,
        // 更新
        UPDATE,
    }
}
