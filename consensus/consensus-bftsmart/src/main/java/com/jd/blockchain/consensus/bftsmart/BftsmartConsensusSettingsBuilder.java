package com.jd.blockchain.consensus.bftsmart;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;

import com.jd.blockchain.consensus.ConsensusSettingsBuilder;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NetworkReplica;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;

import utils.PropertiesUtils;
import utils.Property;
import utils.codec.Base58Utils;
import utils.io.BytesUtils;
import utils.net.NetworkAddress;

public class BftsmartConsensusSettingsBuilder implements ConsensusSettingsBuilder {

	private static final int DEFAULT_TXSIZE = 1000;

	private static final int DEFAULT_MAXDELAY = 1000;

	private static final String CONFIG_TEMPLATE_FILE = "bftsmart.config";

	private static final String CONFIG_LEDGER_INIT = "ledger.init";

	public static final String PARTICIPANT_OP_KEY = "participant.op";

	public static final String DEACTIVE_PARTICIPANT_ID_KEY = "deactive.participant.id";

	public static final String ACTIVE_PARTICIPANT_ID_KEY = "active.participant.id";

	/**
	 * 参数键：节点数量；
	 */
	public static final String SERVER_NUM_KEY = "system.servers.num";

	/**
	 * 参数键：拜占庭节点数量；
	 */
	public static final String F_NUM_KEY = "system.servers.f";

	/**
	 * 参数键：共识节点视图信息
	 */
	public static final String SERVER_VIEW_KEY = "system.initial.view";

	/**
	 * 参数键：结块条件设置；
	 */
	public static final String BFTSMART_BLOCK_TXSIZE_KEY = "system.block.txsize";

	public static final String BFTSMART_BLOCK_MAXDELAY_KEY = "system.block.maxdelay";

	// /**
	// * 参数键格式：节点地址；
	// */
	// public static final String ADDRESS_PATTERN = "node.%s.address";

	/**
	 * 参数键格式：节点公钥；
	 */
	public static final String PUBKEY_PATTERN = "system.server.%s.pubkey";

	/**
	 * 参数键格式：节点共识服务的网络地址；
	 */
	public static final String CONSENSUS_HOST_PATTERN = "system.server.%s.network.host";

	/**
	 * 参数键格式：节点共识服务的端口；
	 */
	public static final String CONSENSUS_PORT_PATTERN = "system.server.%s.network.port";

	/**
	 * 参数键格式：节点共识服务的通讯是否开启安全选项；
	 */
	public static final String CONSENSUS_SECURE_PATTERN = "system.server.%s.network.secure";

	public static final String BFTSMART_PROVIDER = "com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider";

	private static Properties CONFIG_TEMPLATE;
	static {
		ClassPathResource configResource = new ClassPathResource(CONFIG_TEMPLATE_FILE);
		try {
			try (InputStream in = configResource.getInputStream()) {
				CONFIG_TEMPLATE = PropertiesUtils.load(in, BytesUtils.DEFAULT_CHARSET);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	// 解析得到结块的相关配置信息
//	public BftsmartCommitBlockConfig createBlockConfig(Properties resolvingProps) {
//		BftsmartCommitBlockConfig blockConfig = new BftsmartCommitBlockConfig();
//
//		String txSizeString = PropertiesUtils.getRequiredProperty(resolvingProps, BFTSMART_BLOCK_TXSIZE_KEY);
//		resolvingProps.remove(BFTSMART_BLOCK_TXSIZE_KEY);
//
//		if (txSizeString == null || txSizeString.length() == 0) {
//			blockConfig.setTxSizePerBlock(DEFAULT_TXSIZE);
//		}
//		else {
//			blockConfig.setTxSizePerBlock(Integer.parseInt(txSizeString));
//		}
//
//		String maxDelayString = PropertiesUtils.getRequiredProperty(resolvingProps, BFTSMART_BLOCK_MAXDELAY_KEY);
//		resolvingProps.remove(BFTSMART_BLOCK_MAXDELAY_KEY);
//
//		if (maxDelayString == null || maxDelayString.length() == 0) {
//			blockConfig.setMaxDelayMilliSecondsPerBlock(DEFAULT_MAXDELAY);
//		}
//		else {
//			blockConfig.setMaxDelayMilliSecondsPerBlock(Long.parseLong(maxDelayString));
//		}
//
//		return blockConfig;
//	}

	@Override
	public Properties createPropertiesTemplate() {
		return PropertiesUtils.cloneFrom(CONFIG_TEMPLATE);
	}

	@Override
	public BftsmartConsensusViewSettings createSettings(Properties props, Replica[] participantNodes) {
		Properties resolvingProps = PropertiesUtils.cloneFrom(props);
		int serversNum = PropertiesUtils.getInt(resolvingProps, SERVER_NUM_KEY);
		if (serversNum < 0) {
			throw new IllegalArgumentException(String.format("Property[%s] is negative!", SERVER_NUM_KEY));
		}
		if (serversNum < 4) {
			throw new IllegalArgumentException(String.format("Property[%s] is less than 4!", SERVER_NUM_KEY));
		}
		if (participantNodes == null) {
			throw new IllegalArgumentException("ParticipantNodes is Empty !!!");
		}
//		if (serversNum != participantNodes.length) {
//			throw new IllegalArgumentException(
//					String.format("Property[%s] which is [%s] unequal " + "ParticipantNodes's length which is [%s] !",
//							SERVER_NUM_KEY, serversNum, participantNodes.length));
//		}
		serversNum = participantNodes.length;

//		BftsmartCommitBlockConfig blockConfig = createBlockConfig(resolvingProps);

		BftsmartNodeSettings[] nodesSettings = new BftsmartNodeSettings[serversNum];
		for (int i = 0; i < serversNum; i++) {
			int id = i;

//			String keyOfPubkey = keyOfNode(PUBKEY_PATTERN, id);
//			String base58PubKey = PropertiesUtils.getRequiredProperty(resolvingProps, keyOfPubkey);
//			PubKey pubKey = new PubKey(Base58Utils.decode(base58PubKey));
//			PubKey pubKey = KeyGenCommand.decodePubKey(base58PubKey);
			PubKey pubKey = participantNodes[i].getPubKey();
//			resolvingProps.remove(keyOfPubkey);
			
			String keyOfHost = keyOfNode(CONSENSUS_HOST_PATTERN, participantNodes[id].getId());
			String networkAddressHost = PropertiesUtils.getOptionalProperty(resolvingProps, keyOfHost, null);
			resolvingProps.remove(keyOfHost);

			String keyOfPort = keyOfNode(CONSENSUS_PORT_PATTERN, participantNodes[id].getId());
			int networkAddressPort = PropertiesUtils.getIntOptional(resolvingProps, keyOfPort, -1);
			resolvingProps.remove(keyOfPort);

			String keyOfSecure = keyOfNode(CONSENSUS_SECURE_PATTERN, participantNodes[id].getId());
			boolean networkAddressSecure = PropertiesUtils.getBooleanOptional(resolvingProps, keyOfSecure, false);
			resolvingProps.remove(keyOfSecure);
			
			if (participantNodes[i] instanceof NetworkReplica) {
				NetworkReplica replica = (NetworkReplica) participantNodes[i];
				networkAddressHost = replica.getNetworkAddress().getHost();
				networkAddressPort = replica.getNetworkAddress().getPort();
				networkAddressSecure = replica.getNetworkAddress().isSecure();
			}

			BftsmartNodeConfig nodeConfig = new BftsmartNodeConfig(pubKey, participantNodes[id].getId(),
					new NetworkAddress(networkAddressHost, networkAddressPort, networkAddressSecure));
			nodesSettings[i] = nodeConfig;
		}

		BftsmartConsensusConfig config = new BftsmartConsensusConfig(nodesSettings,
//				blockConfig,
				PropertiesUtils.getOrderedValues(resolvingProps), 0);
		return config;
	}

	public static String keyOfNode(String pattern, int id) {
		return String.format(pattern, id);
	}

	@Override
	public Properties convertToProperties(ConsensusViewSettings settings) {
		Properties props = new Properties();
		int serversNum = PropertiesUtils.getInt(props, SERVER_NUM_KEY);
		if (serversNum > 0) {
			for (int i = 0; i < serversNum; i++) {
				int id = i;
//				String keyOfPubkey = keyOfNode(PUBKEY_PATTERN, id);
//				props.remove(keyOfPubkey);

				String keyOfHost = keyOfNode(CONSENSUS_HOST_PATTERN, id);
				props.remove(keyOfHost);

				String keyOfPort = keyOfNode(CONSENSUS_PORT_PATTERN, id);
				props.remove(keyOfPort);

				String keyOfSecure = keyOfNode(CONSENSUS_SECURE_PATTERN, id);
				props.remove(keyOfSecure);
			}
		}

		BftsmartConsensusViewSettings bftsmartSettings = (BftsmartConsensusViewSettings) settings;
		BftsmartNodeSettings[] nodesSettings = (BftsmartNodeSettings[]) bftsmartSettings.getNodes();
		serversNum = nodesSettings.length;
		props.setProperty(SERVER_NUM_KEY, serversNum + "");

		// 获得结块相关的属性信息
//		BftsmartCommitBlockSettings blockSettings = bftsmartSettings.getCommitBlockSettings();
//		if (blockSettings == null) {
//			props.setProperty(BFTSMART_BLOCK_TXSIZE_KEY, DEFAULT_TXSIZE + "");
//			props.setProperty(BFTSMART_BLOCK_MAXDELAY_KEY, DEFAULT_MAXDELAY + "");
//		} else {
//			int txSize = blockSettings.getTxSizePerBlock();
//			long maxDelay = blockSettings.getMaxDelayMilliSecondsPerBlock();
//			props.setProperty(BFTSMART_BLOCK_TXSIZE_KEY, txSize + "");
//			props.setProperty(BFTSMART_BLOCK_MAXDELAY_KEY, maxDelay + "");
//		}

		for (int i = 0; i < serversNum; i++) {
			BftsmartNodeSettings ns = nodesSettings[i];
			int id = i;
//			String keyOfPubkey = keyOfNode(PUBKEY_PATTERN, id);
//			props.setProperty(keyOfPubkey, ns.getPubKey().toBase58());

			String keyOfHost = keyOfNode(CONSENSUS_HOST_PATTERN, id);
			props.setProperty(keyOfHost, ns.getNetworkAddress() == null ? "" : ns.getNetworkAddress().getHost());

			String keyOfPort = keyOfNode(CONSENSUS_PORT_PATTERN, id);
			props.setProperty(keyOfPort, ns.getNetworkAddress() == null ? "" : ns.getNetworkAddress().getPort() + "");

			String keyOfSecure = keyOfNode(CONSENSUS_SECURE_PATTERN, id);
			props.setProperty(keyOfSecure,
					ns.getNetworkAddress() == null ? "false" : ns.getNetworkAddress().isSecure() + "");
		}

		PropertiesUtils.setValues(props, bftsmartSettings.getSystemConfigs());

		return props;
	}

	@Override
	public ConsensusViewSettings updateSettings(ConsensusViewSettings oldConsensusSettings, Properties newProps) {

		if (newProps != null) {
			// update system config and node settings
			Property[] systemConfigs = modifySystemProperties(
					((BftsmartConsensusViewSettings) oldConsensusSettings).getSystemConfigs(), newProps);

			BftsmartNodeSettings[] newNodeSettings = createNewNodeSetting(oldConsensusSettings.getNodes(), newProps);

			if (newProps.getProperty(PARTICIPANT_OP_KEY) != null) {
				return new BftsmartConsensusConfig(newNodeSettings, systemConfigs,
						((BftsmartConsensusViewSettings) oldConsensusSettings).getViewId() + 1);
			} else {
				return new BftsmartConsensusConfig(newNodeSettings, systemConfigs,
						((BftsmartConsensusViewSettings) oldConsensusSettings).getViewId());
			}
		} else {
			throw new IllegalArgumentException("updateSettings parameters error!");
		}

	}

	private BftsmartNodeSettings[] createNewNodeSetting(NodeSettings[] oldNodeSettings, Properties newProps) {

		BftsmartNodeSettings[] bftsmartNodeSettings = null;

		if (newProps.getProperty(PARTICIPANT_OP_KEY) != null) {

			if (newProps.getProperty(PARTICIPANT_OP_KEY).equals("active")) {

				// organize new participant node
				int activeId = Integer.parseInt(newProps.getProperty(ACTIVE_PARTICIPANT_ID_KEY));
				String host = newProps.getProperty(keyOfNode(CONSENSUS_HOST_PATTERN, activeId));
				int port = Integer.parseInt(newProps.getProperty(keyOfNode(CONSENSUS_PORT_PATTERN, activeId)));
				boolean secure = Boolean.parseBoolean(newProps.getProperty(keyOfNode(CONSENSUS_SECURE_PATTERN, activeId)));
				byte[] pubKeyBytes = Base58Utils.decode(newProps.getProperty(keyOfNode(PUBKEY_PATTERN, activeId)));
				PubKey pubKey = Crypto.resolveAsPubKey(pubKeyBytes);
				BftsmartNodeConfig bftsmartNodeConfig = new BftsmartNodeConfig(pubKey, activeId, new NetworkAddress(host, port, secure));

				int index = oldNodeSettings.length;
				for(int i = 0; i < oldNodeSettings.length; i ++) {
					NodeSettings settings = oldNodeSettings[i];
					if(settings.getAddress().equals(bftsmartNodeConfig.getAddress())) {
						index = i;
					}
				}
				if(index == oldNodeSettings.length) {
					bftsmartNodeSettings = new BftsmartNodeSettings[oldNodeSettings.length + 1];
				} else {
					bftsmartNodeSettings = new BftsmartNodeSettings[oldNodeSettings.length];
				}
				for (int i = 0; i < oldNodeSettings.length; i++) {
					bftsmartNodeSettings[i] = (BftsmartNodeSettings) oldNodeSettings[i];
				}
				bftsmartNodeSettings[index] = bftsmartNodeConfig;

			} else if (newProps.getProperty(PARTICIPANT_OP_KEY).equals("deactive")) {
				int deActiveId = Integer.parseInt(newProps.getProperty(DEACTIVE_PARTICIPANT_ID_KEY));

				bftsmartNodeSettings = new BftsmartNodeSettings[oldNodeSettings.length - 1];
				int j = 0;
				for (int i = 0; i < oldNodeSettings.length; i++) {
					BftsmartNodeSettings bftsmartNodeSetting = (BftsmartNodeSettings) oldNodeSettings[i];
					if (bftsmartNodeSetting.getId() != deActiveId) {
						bftsmartNodeSettings[j++] = bftsmartNodeSetting;
					}
				}
			} else {
				throw new IllegalArgumentException("createNewNodeSetting properties error!");
			}
		} else {
			bftsmartNodeSettings = new BftsmartNodeSettings[oldNodeSettings.length];
			for (int i = 0; i < oldNodeSettings.length; i++) {
				bftsmartNodeSettings[i] = (BftsmartNodeSettings) oldNodeSettings[i];
			}
		}

		return bftsmartNodeSettings;
	}

	/**
	 *
	 * update consensus system properties
	 *
	 */
	private Property[] modifySystemProperties(Property[] systemProperties, Properties newProps) {

		Map<String, Property> propertyMapOrig = convert2Map(systemProperties);

		Map<String, Property> propertyMapNew = new HashMap<>();

		Property[] properties = PropertiesUtils.getOrderedValues(newProps);

		if (newProps.getProperty(PARTICIPANT_OP_KEY) != null) {

			if (newProps.getProperty(PARTICIPANT_OP_KEY).equals("deactive")) {
				String deActiveId = newProps.getProperty(DEACTIVE_PARTICIPANT_ID_KEY);

				for (String key : propertyMapOrig.keySet()) {
					if (key.startsWith(keyOfNode(CONSENSUS_HOST_PATTERN, Integer.parseInt(deActiveId)))
							|| key.startsWith(keyOfNode(CONSENSUS_PORT_PATTERN, Integer.parseInt(deActiveId)))
							|| key.startsWith(keyOfNode(CONSENSUS_SECURE_PATTERN, Integer.parseInt(deActiveId)))
							|| key.startsWith(keyOfNode(PUBKEY_PATTERN, Integer.parseInt(deActiveId)))) {
						continue;
					}
					propertyMapNew.put(key, propertyMapOrig.get(key));
				}

				for (Property property : properties) {
					propertyMapNew.put(property.getName(), new Property(property.getName(), property.getValue()));
				}
				return convert2Array(propertyMapNew);
			}
		}

		propertyMapNew = propertyMapOrig;
		for (Property property : properties) {
			propertyMapNew.put(property.getName(), new Property(property.getName(), property.getValue()));
		}

		return convert2Array(propertyMapNew);
	}

	private Map<String, Property> convert2Map(Property[] properties) {
		Map<String, Property> propertyMap = new HashMap<>();
		for (Property property : properties) {
			propertyMap.put(property.getName(), property);
		}
		return propertyMap;
	}

	private Property[] convert2Array(Map<String, Property> map) {
		Property[] properties = new Property[map.size()];
		int index = 0;
		for (Map.Entry<String, Property> entry : map.entrySet()) {
			properties[index++] = entry.getValue();
		}
		return properties;
	}

	@Override
	public ConsensusViewSettings addReplicaSetting(ConsensusViewSettings viewSettings, Replica replica) {
		if (!(viewSettings instanceof BftsmartConsensusViewSettings)) {
			throw new IllegalArgumentException("The specified view-settings is not a bftsmart-consensus-settings!");
		}
		if (!(replica instanceof BftsmartReplica)) {
			throw new IllegalArgumentException("The specified replica is not a bftsmart-replica!");
		}
		BftsmartConsensusViewSettings bftsmartSettings = (BftsmartConsensusViewSettings) viewSettings;
		BftsmartReplica newReplica = (BftsmartReplica) replica;

		NodeSettings[] origNodes = bftsmartSettings.getNodes();
		if (origNodes.length == 0) {
			throw new IllegalStateException("The number of nodes is zero!");
		}

		// 更新节点列表；
		BftsmartNodeSettings[] newNodes = new BftsmartNodeSettings[origNodes.length + 1];
		for (int i = 0; i < origNodes.length; i++) {
			newNodes[i] = (BftsmartNodeSettings) origNodes[i];
		}
		newNodes[origNodes.length] = new BftsmartNodeConfig(newReplica.getPubKey(), newReplica.getId(),
				newReplica.getNetworkAddress());

		// 更新系统属性；
		Properties systemProps = PropertiesUtils.createProperties(bftsmartSettings.getSystemConfigs());

		systemProps.setProperty(SERVER_NUM_KEY, String.valueOf(newNodes.length));

		int f = computeBFTNumber(newNodes.length);
		systemProps.setProperty(F_NUM_KEY, String.valueOf(f));

		String[] processIds = new String[newNodes.length];
		for (int i = 0; i < processIds.length; i++) {
			processIds[i] = String.valueOf(newNodes[i].getId());
		}
		String viewPIDs = String.join(",", processIds);
		systemProps.setProperty(SERVER_VIEW_KEY, viewPIDs);

		// viewID increment;
		int viewId = bftsmartSettings.getViewId() + 1;

		return new BftsmartConsensusConfig(newNodes, PropertiesUtils.getOrderedValues(systemProps), viewId);
	}

	/**
	 * 计算在指定节点数量下的拜占庭容错数；
	 * 
	 * @param nodeNumber
	 * @return
	 */
	public static int computeBFTNumber(int nodeNumber) {
		if (nodeNumber < 1) {
			throw new IllegalArgumentException("Node number is less than 1!");
		}
		int f = 0;
		while (true) {
			if (nodeNumber >= (3 * f + 1) && nodeNumber < (3 * (f + 1) + 1)) {
				break;
			}
			f++;
		}
		return f;
	}

}
