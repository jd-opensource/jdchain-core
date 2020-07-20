package com.jd.blockchain.consensus.bftsmart;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.utils.PropertiesUtils;
import com.jd.blockchain.utils.Property;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;
import com.jd.blockchain.utils.net.NetworkAddress;

import org.springframework.core.io.ClassPathResource;

import com.jd.blockchain.consensus.ConsensusSettings;
import com.jd.blockchain.consensus.ConsensusSettingsBuilder;
import com.jd.blockchain.crypto.PubKey;


public class BftsmartConsensusSettingsBuilder implements ConsensusSettingsBuilder {

	private static final int DEFAULT_TXSIZE = 1000;

	private static final int DEFAULT_MAXDELAY = 1000;

	private static final String CONFIG_TEMPLATE_FILE = "bftsmart.config";

	private static final String CONFIG_LEDGER_INIT = "ledger.init";

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

	public static final  String  BFTSMART_PROVIDER = "com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider";



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

	//解析得到结块的相关配置信息
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
	public BftsmartConsensusSettings createSettings(Properties props, ParticipantNode[] participantNodes) {
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
		if (serversNum != participantNodes.length) {
			throw new IllegalArgumentException(String.format("Property[%s] which is [%s] unequal " +
					"ParticipantNodes's length which is [%s] !", SERVER_NUM_KEY, serversNum, participantNodes.length));
		}

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

			String keyOfHost = keyOfNode(CONSENSUS_HOST_PATTERN, id);
			String networkAddressHost = PropertiesUtils.getRequiredProperty(resolvingProps, keyOfHost);
			resolvingProps.remove(keyOfHost);

			String keyOfPort = keyOfNode(CONSENSUS_PORT_PATTERN, id);
			int networkAddressPort = PropertiesUtils.getInt(resolvingProps, keyOfPort);
			resolvingProps.remove(keyOfPort);

			String keyOfSecure = keyOfNode(CONSENSUS_SECURE_PATTERN, id);
			boolean networkAddressSecure = PropertiesUtils.getBoolean(resolvingProps, keyOfSecure);
			resolvingProps.remove(keyOfSecure);

			BftsmartNodeConfig nodeConfig = new BftsmartNodeConfig(pubKey, id,
					new NetworkAddress(networkAddressHost, networkAddressPort, networkAddressSecure));
			nodesSettings[i] = nodeConfig;
		}

		BftsmartConsensusConfig config = new BftsmartConsensusConfig(nodesSettings,
//				blockConfig,
				PropertiesUtils.getOrderedValues(resolvingProps), 0);
		return config;
	}

	private static String keyOfNode(String pattern, int id) {
		return String.format(pattern, id);
	}

	@Override
	public void writeSettings(ConsensusSettings settings, Properties props) {
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

		BftsmartConsensusSettings bftsmartSettings = (BftsmartConsensusSettings) settings;
		BftsmartNodeSettings[] nodesSettings = (BftsmartNodeSettings[]) bftsmartSettings.getNodes();
		serversNum = nodesSettings.length;
		props.setProperty(SERVER_NUM_KEY, serversNum + "");

		//获得结块相关的属性信息
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
			props.setProperty(keyOfSecure, ns.getNetworkAddress() == null ? "false" : ns.getNetworkAddress().isSecure() + "");
		}

		PropertiesUtils.setValues(props, bftsmartSettings.getSystemConfigs());
	}

	@Override
	public ConsensusSettings updateSettings(ConsensusSettings oldConsensusSettings, Properties newProps) {

		// update system config and node settings
		Property[] systemConfigs = modifySystemProperties(((BftsmartConsensusSettings) oldConsensusSettings).getSystemConfigs(), newProps);
		BftsmartNodeSettings[] newNodeSettings = createNewNodeSetting(systemConfigs);

		if (newProps != null) {
			if (newProps.getProperty(SERVER_VIEW_KEY) != null) {

				// when active new participant, update view id
				return new BftsmartConsensusConfig(newNodeSettings, systemConfigs, ((BftsmartConsensusSettings) oldConsensusSettings).getViewId() + 1);
			} else {
				return new BftsmartConsensusConfig(newNodeSettings, systemConfigs, ((BftsmartConsensusSettings) oldConsensusSettings).getViewId());
			}
		} else {
			throw new IllegalArgumentException("updateSettings parameters error!");
		}

	}

	private BftsmartNodeSettings[] createNewNodeSetting(Property[] systemConfigs) {

		Properties properties = PropertiesUtils.createProperties(systemConfigs);

		String serversView = properties.getProperty(SERVER_VIEW_KEY);

		StringTokenizer str = new StringTokenizer(serversView, ",");
		int[] initialView = new int[str.countTokens()];

		for (int i = 0; i < initialView.length; i++) {
			initialView[i] = Integer.parseInt(str.nextToken());
		}

		BftsmartNodeSettings[] nodesSettings = new BftsmartNodeSettings[initialView.length];

		for (int i = 0; i < initialView.length; i++) {
			int id = initialView[i];

			String keyOfHost = keyOfNode(CONSENSUS_HOST_PATTERN, id);
			String networkAddressHost = PropertiesUtils.getRequiredProperty(properties, keyOfHost);

			String keyOfPort = keyOfNode(CONSENSUS_PORT_PATTERN, id);
			int networkAddressPort = PropertiesUtils.getInt(properties, keyOfPort);

			String keyOfSecure = keyOfNode(CONSENSUS_SECURE_PATTERN, id);
			boolean networkAddressSecure = PropertiesUtils.getBoolean(properties, keyOfSecure);

			String keyOfPubkey = keyOfNode(PUBKEY_PATTERN, id);
			String networkAddressPubkey = PropertiesUtils.getRequiredProperty(properties, keyOfPubkey);


			BftsmartNodeConfig nodeConfig = new BftsmartNodeConfig(new PubKey(Base58Utils.decode(networkAddressPubkey)), id,
					new NetworkAddress(networkAddressHost, networkAddressPort, networkAddressSecure));
			nodesSettings[i] = nodeConfig;
		}

		return nodesSettings;
	}

	/**
	 *
	 * update consensus system properties
	 *
	 */
	private Property[] modifySystemProperties(Property[] systemProperties, Properties newProps) {

		Map<String, Property> propertyMap = convert2Map(systemProperties);

		Property[] properties = PropertiesUtils.getOrderedValues(newProps);

		for (Property property: properties) {
			propertyMap.put(property.getName(), new Property(property.getName(), property.getValue()));
		}
		return convert2Array(propertyMap);
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

}
