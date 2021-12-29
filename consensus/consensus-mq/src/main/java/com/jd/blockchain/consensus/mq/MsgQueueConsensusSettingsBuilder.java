/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.MsgQueueConsensusSettingsBuilder
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 下午1:46
 * Description:
 */
package com.jd.blockchain.consensus.mq;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.jd.blockchain.crypto.Crypto;
import org.springframework.core.io.ClassPathResource;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.ConsensusSettingsBuilder;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.mq.config.MsgQueueBlockConfig;
import com.jd.blockchain.consensus.mq.config.MsgQueueConsensusConfig;
import com.jd.blockchain.consensus.mq.config.MsgQueueNetworkConfig;
import com.jd.blockchain.consensus.mq.config.MsgQueueNodeConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueBlockSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNetworkSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PubKey;

import utils.Bytes;
import utils.PropertiesUtils;
import utils.codec.Base58Utils;
import utils.io.BytesUtils;
import utils.io.FileUtils;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MsgQueueConsensusSettingsBuilder implements ConsensusSettingsBuilder {

	private static final String DEFAULT_TOPIC_TX = "tx";

	private static final String DEFAULT_TOPIC_TX_RESULT = "tx-result";

	private static final String DEFAULT_TOPIC_BLOCK = "block";

	private static final String DEFAULT_TOPIC_MSG = "msg";

	private static final String DEFAULT_TOPIC_MSG_RESULT = "msg-result";

	private static final int DEFAULT_TXSIZE = 1000;

	private static final int DEFAULT_MAXDELAY = 100;

	/**
	 *
	 */
	private static final String CONFIG_TEMPLATE_FILE = "mq.config";

	/**
	 * 参数键：节点数量；
	 */
	public static final String SERVER_NUM_KEY = "system.servers.num";

	/**
	 * 参数键格式：节点公钥；
	 */
	public static final String PUBKEY_PATTERN = "system.server.%s.pubkey";

	public static final String MSG_QUEUE_SERVER = "system.msg.queue.server";

	public static final String MSG_QUEUE_TOPIC_TX = "system.msg.queue.topic.tx";

	public static final String MSG_QUEUE_TOPIC_TX_RESULT = "system.msg.queue.topic.tx-result";

	public static final String MSG_QUEUE_TOPIC_BLOCK = "system.msg.queue.topic.block";

	public static final String MSG_QUEUE_TOPIC_MSG = "system.msg.queue.topic.msg";

	public static final String MSG_QUEUE_TOPIC_MSG_RESULT = "system.msg.queue.topic.msg-result";

	public static final String MSG_QUEUE_BLOCK_TXSIZE = "system.msg.queue.block.txsize";

	public static final String MSG_QUEUE_BLOCK_MAXDELAY = "system.msg.queue.block.maxdelay";

	public static final String MSG_QUEUE_PROVIDER = "com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider";

	private static Properties CONFIG_TEMPLATE;

	static {
		if (FileUtils.existFile(CONFIG_TEMPLATE_FILE)) {
			ClassPathResource configResource = new ClassPathResource(CONFIG_TEMPLATE_FILE);
			try {
				try (InputStream in = configResource.getInputStream()) {
					CONFIG_TEMPLATE = PropertiesUtils.load(in, BytesUtils.DEFAULT_CHARSET);
				}
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	@Override
	public MsgQueueConsensusSettings createSettings(Properties props, Replica[] participantNodes) {
		MsgQueueNetworkConfig networkConfig = new MsgQueueNetworkConfig();
		Properties resolvingProps = PropertiesUtils.cloneFrom(props);

		String server = PropertiesUtils.getProperty(resolvingProps, MSG_QUEUE_SERVER, true);
		if (server == null || server.length() <= 0) {
			throw new IllegalArgumentException(String.format("Property[%s] is empty!", MSG_QUEUE_SERVER));
		}
		networkConfig.setServer(server).setTxTopic(initProp(resolvingProps, MSG_QUEUE_TOPIC_TX, DEFAULT_TOPIC_TX))
				.setTxResultTopic(initProp(resolvingProps, MSG_QUEUE_TOPIC_TX_RESULT, DEFAULT_TOPIC_TX_RESULT))
				.setBlockTopic(initProp(resolvingProps, MSG_QUEUE_TOPIC_BLOCK, DEFAULT_TOPIC_BLOCK))
				.setMsgTopic(initProp(resolvingProps, MSG_QUEUE_TOPIC_MSG, DEFAULT_TOPIC_MSG))
				.setMsgResultTopic(initProp(resolvingProps, MSG_QUEUE_TOPIC_MSG_RESULT, DEFAULT_TOPIC_MSG_RESULT));

		MsgQueueBlockConfig blockConfig = new MsgQueueBlockConfig()
				.setTxSizePerBlock(initProp(resolvingProps, MSG_QUEUE_BLOCK_TXSIZE, DEFAULT_TXSIZE))
				.setMaxDelayMilliSecondsPerBlock(initProp(resolvingProps, MSG_QUEUE_BLOCK_MAXDELAY, DEFAULT_MAXDELAY));

		MsgQueueConsensusConfig consensusConfig = new MsgQueueConsensusConfig().setBlockSettings(blockConfig)
				.setNetworkSettings(networkConfig);
		// load node settings
		int serversNum = PropertiesUtils.getInt(resolvingProps, SERVER_NUM_KEY);
		for (int i = 0; i < serversNum; i++) {
			int id = i;

			String keyOfPubkey = nodeKey(PUBKEY_PATTERN, id);

			String base58PubKey = PropertiesUtils.getRequiredProperty(resolvingProps, keyOfPubkey);
			PubKey pubKey = KeyGenUtils.decodePubKey(base58PubKey);

//            PubKey pubKey = new PubKey(Base58Utils.decode(base58PubKey));
			resolvingProps.remove(keyOfPubkey);
			Bytes address = AddressEncoding.generateAddress(pubKey);

			String networkAddress = address.toBase58();
			MsgQueueNodeConfig nodeConfig = new MsgQueueNodeConfig().setAddress(networkAddress).setPubKey(pubKey).setId(id);
			consensusConfig.addNodeSettings(nodeConfig);
		}
		return consensusConfig;
	}

	private MsgQueueNodeSettings[] addNodeSetting(NodeSettings[] nodeSettings, PubKey newParticipantPk) {

		MsgQueueNodeSettings msgQueueNodeSettings = new MsgQueueNodeConfig();
		((MsgQueueNodeConfig) msgQueueNodeSettings)
				.setAddress(AddressEncoding.generateAddress(newParticipantPk).toBase58());
		((MsgQueueNodeConfig) msgQueueNodeSettings).setPubKey(newParticipantPk);

		MsgQueueNodeSettings[] msgQueuetNodeSettings = new MsgQueueNodeSettings[nodeSettings.length + 1];
		for (int i = 0; i < nodeSettings.length; i++) {
			msgQueuetNodeSettings[i] = (MsgQueueNodeSettings) nodeSettings[i];
		}
		msgQueuetNodeSettings[nodeSettings.length] = msgQueueNodeSettings;

		return msgQueuetNodeSettings;
	}

	@Override
	public Properties createPropertiesTemplate() {
		return PropertiesUtils.cloneFrom(CONFIG_TEMPLATE);
	}

	@Override
	public Properties convertToProperties(ConsensusViewSettings settings) {
		Properties props = new Properties();

		if (!(settings instanceof MsgQueueConsensusSettings)) {
			throw new IllegalArgumentException(
					"ConsensusSettings data isn't supported! Accept MsgQueueConsensusSettings only!");
		}

		MsgQueueConsensusSettings consensusSettings = (MsgQueueConsensusSettings) settings;

		MsgQueueNetworkSettings networkSettings = consensusSettings.getNetworkSettings();
		if (networkSettings == null || networkSettings.getServer() == null
				|| networkSettings.getServer().length() <= 0) {
			throw new IllegalArgumentException("MsgQueue Consensus server is empty!");
		}

		String server = networkSettings.getServer();
		props.setProperty(MSG_QUEUE_SERVER, server);

		String txTopic = networkSettings.getTxTopic();
		if (txTopic == null || txTopic.length() <= 0) {
			txTopic = DEFAULT_TOPIC_TX;
		}
		props.setProperty(MSG_QUEUE_TOPIC_TX, txTopic);

		String txResultTopic = networkSettings.getTxResultTopic();
		if (txResultTopic == null || txResultTopic.length() <= 0) {
			txResultTopic = DEFAULT_TOPIC_TX_RESULT;
		}
		props.setProperty(MSG_QUEUE_TOPIC_TX_RESULT, txResultTopic);

		String blockTopic = networkSettings.getBlockTopic();
		if (blockTopic == null || blockTopic.length() <= 0) {
			blockTopic = DEFAULT_TOPIC_BLOCK;
		}
		props.setProperty(MSG_QUEUE_TOPIC_BLOCK, blockTopic);

		String msgTopic = networkSettings.getMsgTopic();
		if (msgTopic == null || msgTopic.length() <= 0) {
			msgTopic = DEFAULT_TOPIC_MSG;
		}
		props.setProperty(MSG_QUEUE_TOPIC_MSG, msgTopic);

		String msgResultTopic = networkSettings.getMsgResultTopic();
		if (msgResultTopic == null || msgResultTopic.length() <= 0) {
			msgResultTopic = DEFAULT_TOPIC_MSG_RESULT;
		}
		props.setProperty(MSG_QUEUE_TOPIC_MSG_RESULT, msgResultTopic);

		MsgQueueBlockSettings blockSettings = consensusSettings.getBlockSettings();
		if (blockSettings == null) {
			props.setProperty(MSG_QUEUE_BLOCK_TXSIZE, DEFAULT_TXSIZE + "");
			props.setProperty(MSG_QUEUE_BLOCK_MAXDELAY, DEFAULT_MAXDELAY + "");
		} else {
			int txSize = blockSettings.getTxSizePerBlock();
			long maxDelay = blockSettings.getMaxDelayMilliSecondsPerBlock();
			props.setProperty(MSG_QUEUE_BLOCK_TXSIZE, txSize + "");
			props.setProperty(MSG_QUEUE_BLOCK_MAXDELAY, maxDelay + "");
		}

		NodeSettings[] nodes = consensusSettings.getNodes();
		props.setProperty(SERVER_NUM_KEY, nodes.length + "");
		for (int i = 0; i < nodes.length; i++) {
			props.setProperty(nodeKey(PUBKEY_PATTERN, i), nodes[i].getPubKey().toString());
		}

		return props;
	}

	@Override
	public ConsensusViewSettings updateSettings(ConsensusViewSettings oldConsensusSettings, Properties newProps) {
		String op = newProps.getProperty("participant.op");
		int id = Integer.valueOf(newProps.getProperty("participant.id"));
		String pubkeyBase58 = newProps.getProperty("system.server."+ id +".pubkey");
		PubKey pubkey = Crypto.resolveAsPubKey(Base58Utils.decode(pubkeyBase58));
		Bytes address = AddressEncoding.generateAddress(pubkey);

		MsgQueueConsensusSettings consensusSettings = (MsgQueueConsensusSettings) oldConsensusSettings;
		NodeSettings[] nodes = consensusSettings.getNodes();
		List<MsgQueueNodeConfig> mqNodes = new ArrayList<>();
		boolean needAdd = true;
		for(NodeSettings settings : nodes) {
			MsgQueueNodeSettings node = (MsgQueueNodeSettings) settings;
			if(node.getPubKey().equals(pubkey)) {
				if(op.equals("deactive")) {
					needAdd = false;
					continue;
				}
			} else {
				MsgQueueNodeConfig nodeConfig = new MsgQueueNodeConfig();
				nodeConfig.setAddress(node.getAddress());
				nodeConfig.setPubKey(node.getPubKey());
				nodeConfig.setId(node.getId());
				mqNodes.add(nodeConfig);
			}
		}
		if(needAdd) {
			MsgQueueNodeConfig nodeConfig = new MsgQueueNodeConfig();
			nodeConfig.setAddress(address.toString());
			nodeConfig.setId(id);
			nodeConfig.setPubKey(pubkey);
			mqNodes.add(nodeConfig);
		}
		MsgQueueConsensusConfig newSettings = new MsgQueueConsensusConfig();
		newSettings.setBlockSettings(consensusSettings.getBlockSettings());
		newSettings.setNetworkSettings(consensusSettings.getNetworkSettings());
		for(MsgQueueNodeConfig node : mqNodes) {
			newSettings.addNodeSettings(node);
		}
		return newSettings;
	}

	private String initProp(Properties resolvingProps, String key, String defaultVal) {
		try {
			String value = PropertiesUtils.getProperty(resolvingProps, key, true);
			if (value == null || value.length() <= 0) {
				value = defaultVal;
			}
			return value;
		} catch (Exception e) {
			return defaultVal;
		}
	}

	private int initProp(Properties resolvingProps, String key, int defaultVal) {
		try {
			int value = PropertiesUtils.getInt(resolvingProps, key);
			if (value <= 0) {
				value = defaultVal;
			}
			return value;
		} catch (Exception e) {
			return defaultVal;
		}
	}

	private static String nodeKey(String pattern, int id) {
		return String.format(pattern, id);
	}

	@Override
	public ConsensusViewSettings addReplicaSetting(ConsensusViewSettings settings, Replica replica) {
		// TODO Auto-generated method stub
		throw new IllegalStateException("Not implemented!");
	}
}