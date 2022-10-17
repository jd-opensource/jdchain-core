package com.jd.blockchain.gateway.service;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusConfig;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeConfig;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.consensus.mq.config.MQBlockConfig;
import com.jd.blockchain.consensus.mq.config.MQConsensusConfig;
import com.jd.blockchain.consensus.mq.config.MQNetworkConfig;
import com.jd.blockchain.consensus.mq.config.MQNodeConfig;
import com.jd.blockchain.consensus.mq.settings.MQBlockSettings;
import com.jd.blockchain.consensus.mq.settings.MQConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MQNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MQNodeSettings;
import com.jd.blockchain.consensus.raft.config.RaftConfig;
import com.jd.blockchain.consensus.raft.config.RaftConsensusConfig;
import com.jd.blockchain.consensus.raft.config.RaftNetworkConfig;
import com.jd.blockchain.consensus.raft.config.RaftNodeConfig;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerMetadata_V2;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.sdk.DecompliedContractInfo;
import com.jd.blockchain.sdk.LedgerInitAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utils.codec.HexUtils;
import utils.query.QueryArgs;
import utils.query.QueryUtils;

import java.util.Arrays;

/**
 * @Author zhaogw
 * @Date 2019/2/22 10:39
 */
@Component
public class GatewayQueryServiceHandler implements GatewayQueryService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

	@Autowired
	private LedgersService peerService;

	@Override
	public HashDigest[] getLedgersHash(int fromIndex, int count) {
		HashDigest[] ledgersHashs = peerService.getLedgerHashs();
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, ledgersHashs.length);
		return Arrays.copyOfRange(ledgersHashs, queryArgs.getFrom(), queryArgs.getFrom() + queryArgs.getCount());
	}

	@Override
	public ParticipantNode[] getConsensusParticipants(HashDigest ledgerHash, int fromIndex, int count) {
		ParticipantNode[] participantNodes = peerService.getQueryService(ledgerHash).getConsensusParticipants(ledgerHash);
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, participantNodes.length);
		ParticipantNode[] participantNodesNews = Arrays.copyOfRange(participantNodes, queryArgs.getFrom(),
				queryArgs.getFrom() + queryArgs.getCount());
		return participantNodesNews;
	}

	@Override
	public LedgerInitAttributes getLedgerBaseSettings(HashDigest ledgerHash) {
		LedgerAdminInfo ledgerAdminInfo = peerService.getQueryService(ledgerHash).getLedgerAdminInfo(ledgerHash);
		return initLedgerBaseSettings(ledgerAdminInfo);
	}

	@Override
	public DecompliedContractInfo getContractSettings(HashDigest ledgerHash, String address) {
		ContractInfo contractInfo = peerService.getQueryService(ledgerHash).getContract(ledgerHash, address);
		return contractSettings(contractInfo);
	}

	private DecompliedContractInfo contractSettings(ContractInfo contractInfo) {
		DecompliedContractInfo contractSettings = new DecompliedContractInfo(contractInfo);
		byte[] chainCodeBytes = contractInfo.getChainCode();

		try {
			// 将反编译chainCode
			String mainClassJava = CONTRACT_PROCESSOR.decompileEntranceClass(chainCodeBytes, contractInfo.getLang());
			contractSettings.setChainCode(mainClassJava);
		} catch (Exception e) {
			// 打印日志
			logger.error(String.format("Decompile contract[%s] error !!!", contractInfo.getAddress().toBase58()), e);
		}

		return contractSettings;
	}

	/**
	 * 初始化账本的基本配置
	 *
	 * @param ledgerAdminInfo 账本信息
	 *
	 * @return
	 */
	private LedgerInitAttributes initLedgerBaseSettings(LedgerAdminInfo ledgerAdminInfo) {

		LedgerMetadata_V2 ledgerMetadata = ledgerAdminInfo.getMetadata();

		LedgerInitAttributes ledgerBaseSettings = new LedgerInitAttributes();
		// 设置参与方
		ledgerBaseSettings.setParticipantNodes(ledgerAdminInfo.getParticipants());
		// 设置共识设置
		ledgerBaseSettings.setConsensusSettings(initConsensusSettings(ledgerAdminInfo));
		// 设置参与方根Hash
		ledgerBaseSettings.setParticipantsHash(ledgerMetadata.getParticipantsHash());
		// 设置算法配置
		ledgerBaseSettings.setCryptoSetting(ledgerAdminInfo.getSettings().getCryptoSetting());
		// 设置种子
		ledgerBaseSettings.setSeed(initSeed(ledgerMetadata.getSeed()));
		// 设置共识协议
		ledgerBaseSettings.setConsensusProtocol(ledgerAdminInfo.getSettings().getConsensusProvider());
		// 设置账本结构版本号
		ledgerBaseSettings.setLedgerStructureVersion(ledgerMetadata.getLedgerStructureVersion());
		// 设置什么认证模式
		ledgerBaseSettings.setIdentityMode(ledgerMetadata.getIdentityMode());
		// 设置账本证书
		ledgerBaseSettings.setLedgerCertificates(ledgerMetadata.getLedgerCertificates());
		// 设置创世用户
		ledgerBaseSettings.setGenesisUsers(ledgerMetadata.getGenesisUsers());
		// 设置合约运行时配置
		ledgerBaseSettings.setContractRuntimeConfig(ledgerMetadata.getContractRuntimeConfig());
		return ledgerBaseSettings;
	}

	/**
	 * 初始化账本种子信息
	 *
	 * @param seedBytes 种子的字节数组显示
	 * @return 种子以十六进制方式显示，为方便阅读，每隔八个字符中间以"-"分割
	 */
	private String initSeed(byte[] seedBytes) {
		String seedString = HexUtils.encode(seedBytes);
		// 每隔八个字符中加入一个一个横线
		StringBuilder seed = new StringBuilder();

		for (int i = 0; i < seedString.length(); i++) {
			char c = seedString.charAt(i);
			if (i != 0 && i % 8 == 0) {
				seed.append("-");
			}
			seed.append(c);
		}

		return seed.toString();
	}

	/**
	 * 初始化共识配置
	 *
	 * @param ledgerAdminInfo 账本元数据
	 * @return
	 */
	private ConsensusViewSettings initConsensusSettings(LedgerAdminInfo ledgerAdminInfo) {
		String consensusProvider = ledgerAdminInfo.getSettings().getConsensusProvider();
		ConsensusProvider provider = ConsensusProviders.getProvider(consensusProvider);
		byte[] consensusSettingsBytes = ledgerAdminInfo.getSettings().getConsensusSetting().toBytes();
		return consensusSettingsDecorator(
				provider.getSettingsFactory().getConsensusSettingsEncoder().decode(consensusSettingsBytes));
	}

	private ConsensusViewSettings consensusSettingsDecorator(ConsensusViewSettings consensusSettings) {
		if (consensusSettings instanceof BftsmartConsensusViewSettings) {
			// bft-smart单独处理
			BftsmartConsensusViewSettings bftsmartConsensusSettings = (BftsmartConsensusViewSettings) consensusSettings;
			NodeSettings[] nodes = bftsmartConsensusSettings.getNodes();
			BftsmartNodeSettings[] bftsmartNodes = null;
			if (nodes != null && nodes.length > 0) {
				bftsmartNodes = new BftsmartNodeSettings[nodes.length];
				for (int i = 0; i < nodes.length; i++) {
					NodeSettings node = nodes[i];
					if (node instanceof BftsmartNodeSettings) {
						BftsmartNodeSettings bftsmartNodeSettings = (BftsmartNodeSettings) node;
						bftsmartNodes[i] = new BftsmartNodeConfig(bftsmartNodeSettings.getPubKey(),
								bftsmartNodeSettings.getId(), bftsmartNodeSettings.getNetworkAddress());

					}
				}
			}
			return new BftsmartConsensusConfig(bftsmartNodes, bftsmartConsensusSettings.getSystemConfigs(), 0);
		} else if (consensusSettings instanceof RaftConsensusSettings) {
			RaftConsensusSettings raftConsensusSettings = (RaftConsensusSettings) consensusSettings;
			NodeSettings[] nodes = raftConsensusSettings.getNodes();
			RaftNodeSettings[] raftNodeSettings = new RaftNodeSettings[0];
			if (nodes != null && nodes.length > 0) {
				raftNodeSettings = new RaftNodeSettings[nodes.length];
				for (int i = 0; i < nodes.length; i++) {
					NodeSettings node = nodes[i];
					if (node instanceof RaftNodeSettings) {
						RaftNodeSettings raftNodeSetting = (RaftNodeSettings) node;
						raftNodeSettings[i] = new RaftNodeConfig(raftNodeSetting.getId(),
								raftNodeSetting.getAddress(), raftNodeSetting.getPubKey(), raftNodeSetting.getNetworkAddress());

					}
				}
			}

			RaftConsensusConfig raftConsensusConfig = new RaftConsensusConfig();
			RaftConfig raftConfig = new RaftConfig();
			RaftNetworkConfig raftNetworkConfig = new RaftNetworkConfig();

			BeanUtils.copyProperties(raftConsensusSettings, raftConsensusConfig);
			BeanUtils.copyProperties(raftConsensusSettings.getRaftSettings(), raftConfig);
			BeanUtils.copyProperties(raftConsensusSettings.getNetworkSettings(), raftNetworkConfig);

			raftConsensusConfig.setNodeSettingsList(Arrays.asList(raftNodeSettings));
			raftConsensusConfig.setRaftSettings(raftConfig);
			raftConsensusConfig.setNetworkSettings(raftNetworkConfig);


			return raftConsensusConfig;
		}else if(consensusSettings instanceof MQConsensusSettings) {
			MQConsensusSettings mqConsensusSettings = (MQConsensusSettings) consensusSettings;
			MQConsensusConfig mqConsensusConfig = new MQConsensusConfig();
			MQBlockSettings blockSettings = mqConsensusSettings.getBlockSettings();
			MQBlockConfig mqQueueBlockConfig = new MQBlockConfig();
			mqQueueBlockConfig.setMaxDelayMilliSecondsPerBlock(blockSettings.getMaxDelayMilliSecondsPerBlock());
			mqQueueBlockConfig.setTxSizePerBlock(blockSettings.getTxSizePerBlock());
			mqConsensusConfig.setBlockSettings(mqQueueBlockConfig);

			MQNetworkSettings networkSettings = mqConsensusSettings.getNetworkSettings();
			MQNetworkConfig mqQueueNetworkConfig = new MQNetworkConfig();
			mqQueueNetworkConfig.setBlockTopic(networkSettings.getBlockTopic());
			mqQueueNetworkConfig.setMsgResultTopic(networkSettings.getMsgResultTopic());
			mqQueueNetworkConfig.setMsgTopic(networkSettings.getMsgTopic());
			mqQueueNetworkConfig.setServer(networkSettings.getServer());
			mqQueueNetworkConfig.setTxTopic(networkSettings.getTxTopic());
			mqQueueNetworkConfig.setTxResultTopic(networkSettings.getTxResultTopic());
			mqConsensusConfig.setNetworkSettings(mqQueueNetworkConfig);

			for(int i=0; i<mqConsensusSettings.getNodes().length; i++) {
				MQNodeSettings nodeSettings = (MQNodeSettings)mqConsensusSettings.getNodes()[i];
				MQNodeConfig msgQueueNodeConfig = new MQNodeConfig();
				msgQueueNodeConfig.setAddress(nodeSettings.getAddress());
				msgQueueNodeConfig.setPubKey(nodeSettings.getPubKey());
				msgQueueNodeConfig.setId(nodeSettings.getId());
				msgQueueNodeConfig.setHost(nodeSettings.getHost());
				mqConsensusConfig.addNodeSettings(msgQueueNodeConfig);
			}

			return mqConsensusConfig;
		}
		return consensusSettings;
	}
}
